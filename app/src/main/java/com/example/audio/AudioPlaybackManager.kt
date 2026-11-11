package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class AudioPlaybackManager {

    companion object {
        private const val TAG = "AudioPlaybackManager"
        const val DEFAULT_SAMPLE_RATE = 24000
    }

    private var audioTrack: AudioTrack? = null
    private val audioQueue = LinkedBlockingQueue<ByteArray>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var playbackJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _outputAmplitude = MutableStateFlow(0f)
    val outputAmplitude: StateFlow<Float> = _outputAmplitude.asStateFlow()

    @Volatile
    private var isInterrupted = false

    init {
        initAudioTrack(DEFAULT_SAMPLE_RATE)
        startPlaybackLoop()
    }

    private fun initAudioTrack(sampleRate: Int) {
        try {
            audioTrack?.release()

            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = max(minBufferSize * 2, 8192)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack: ${e.message}", e)
        }
    }

    private fun startPlaybackLoop() {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            while (isActive) {
                try {
                    val chunk = audioQueue.take()
                    if (isInterrupted) {
                        continue
                    }

                    _isPlaying.value = true
                    calculateAmplitude(chunk)

                    val track = audioTrack
                    if (track != null && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        var offset = 0
                        while (offset < chunk.size && !isInterrupted) {
                            val written = track.write(chunk, offset, chunk.size - offset)
                            if (written <= 0) break
                            offset += written
                        }
                    }

                    if (audioQueue.isEmpty()) {
                        _isPlaying.value = false
                        _outputAmplitude.value = 0f
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error in playback loop: ${e.message}")
                }
            }
        }
    }

    fun enqueueAudio(pcmBytes: ByteArray, sampleRate: Int = DEFAULT_SAMPLE_RATE) {
        if (isInterrupted) {
            // Reset interruption state when fresh audio arrives
            isInterrupted = false
        }
        audioQueue.offer(pcmBytes)
    }

    /**
     * Immediately stops playback, flushes audio buffers, and resets playing status.
     * Called when the user starts speaking or when Gemini Live sends interrupted signal.
     */
    fun interrupt() {
        isInterrupted = true
        audioQueue.clear()
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing AudioTrack: ${e.message}")
        }
        _isPlaying.value = false
        _outputAmplitude.value = 0f
    }

    private fun calculateAmplitude(bytes: ByteArray) {
        if (bytes.size < 2) return
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var sumSquares = 0.0
        val sampleCount = bytes.size / 2

        for (i in 0 until sampleCount) {
            val sample = buffer.short.toDouble()
            sumSquares += sample * sample
        }

        val rms = sqrt(sumSquares / sampleCount)
        // Normalize 0..32767 to 0..1
        val normalized = (rms / 32767.0).toFloat().coerceIn(0f, 1f)
        _outputAmplitude.value = normalized
    }

    fun release() {
        playbackJob?.cancel()
        audioQueue.clear()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioTrack: ${e.message}")
        }
        audioTrack = null
    }
}
