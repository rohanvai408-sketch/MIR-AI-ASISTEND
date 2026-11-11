package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class AudioRecordManager(
    private val wakeWordDetector: WakeWordDetector,
    private val onAudioChunkCaptured: (ByteArray) -> Unit
) {
    companion object {
        private const val TAG = "AudioRecordManager"
        const val SAMPLE_RATE = 16000
    }

    private var audioRecord: AudioRecord? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var recordJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _inputAmplitude = MutableStateFlow(0f)
    val inputAmplitude: StateFlow<Float> = _inputAmplitude.asStateFlow()

    @Volatile
    var streamToGemini: Boolean = false

    @SuppressLint("MissingPermission")
    fun startRecording(): Boolean {
        if (_isRecording.value) return true

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = minBufferSize.coerceAtLeast(4096)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                // Fallback to MIC if VOICE_RECOGNITION fails on device/emulator
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord could not be initialized")
                return false
            }

            audioRecord?.startRecording()
            _isRecording.value = true

            recordJob = scope.launch {
                val readBuffer = ByteArray(2048) // ~64ms of audio at 16kHz 16-bit mono
                while (isActive && _isRecording.value) {
                    val bytesRead = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: -1
                    if (bytesRead > 0) {
                        val chunk = readBuffer.copyOf(bytesRead)
                        calculateInputAmplitude(chunk)

                        // Run local wake-word analysis
                        wakeWordDetector.processAudioFrame(chunk, SAMPLE_RATE)

                        // If currently in active live session, stream PCM to Gemini Live
                        if (streamToGemini) {
                            onAudioChunkCaptured(chunk)
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AudioRecord: ${e.message}", e)
            _isRecording.value = false
            return false
        }
    }

    private fun calculateInputAmplitude(bytes: ByteArray) {
        val sampleCount = bytes.size / 2
        if (sampleCount == 0) return

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var sumSquares = 0.0

        for (i in 0 until sampleCount) {
            val sample = buffer.short.toDouble()
            sumSquares += sample * sample
        }

        val rms = sqrt(sumSquares / sampleCount)
        val normalized = (rms / 32767.0).toFloat().coerceIn(0f, 1f)
        _inputAmplitude.value = normalized
    }

    fun stopRecording() {
        _isRecording.value = false
        streamToGemini = false
        recordJob?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord: ${e.message}")
        }
        audioRecord = null
        _inputAmplitude.value = 0f
    }
}
