package com.example.audio

import android.os.SystemClock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Lightweight real-time acoustic pattern detector for the wake-word "Mir".
 * Detects the acoustic profile of /m/ (low-frequency nasal onset) -> /ɪ/ (vowel energy peak) -> /r/ (decay).
 */
class WakeWordDetector(
    var sensitivity: Float = 0.7f,
    var isEnabled: Boolean = true,
    private val onWakeWordDetected: () -> Unit
) {
    private var lastTriggerTime = 0L
    private val cooldownMs = 2500L // Prevent repetitive triggering

    // State machine for "M-I-R" profile
    private enum class State {
        SEARCHING,
        M_NASAL_ONSET,
        I_VOWEL_PEAK
    }

    private var currentState = State.SEARCHING
    private var stateStartTime = 0L
    private val frameHistory = ArrayDeque<FrameFeatures>()
    private val maxHistoryFrames = 10 // ~500ms history

    data class FrameFeatures(
        val rms: Float,
        val zcr: Float,
        val lowFreqRatio: Float,
        val timestamp: Long
    )

    fun processAudioFrame(bytes: ByteArray, sampleRate: Int = 16000) {
        if (!isEnabled) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastTriggerTime < cooldownMs) return

        val features = extractFeatures(bytes, sampleRate, now)
        frameHistory.addLast(features)
        if (frameHistory.size > maxHistoryFrames) {
            frameHistory.removeFirst()
        }

        // Adjustable threshold based on user sensitivity (0.1 to 1.0)
        // Sensitivity 0.7 -> threshold multiplier ~0.035
        val baseThreshold = (1.05f - sensitivity).coerceIn(0.1f, 1.0f) * 0.045f

        when (currentState) {
            State.SEARCHING -> {
                // Look for /m/ onset: medium energy, strong low-frequency dominance, low zero-crossing rate
                if (features.rms > baseThreshold * 0.7f &&
                    features.lowFreqRatio > 0.55f &&
                    features.zcr < 0.25f
                ) {
                    currentState = State.M_NASAL_ONSET
                    stateStartTime = now
                }
            }
            State.M_NASAL_ONSET -> {
                val elapsed = now - stateStartTime
                if (elapsed > 450) {
                    // Timed out, reset
                    currentState = State.SEARCHING
                } else if (features.rms > baseThreshold * 1.5f && features.zcr < 0.35f) {
                    // Transition to /ɪ/ vowel peak
                    currentState = State.I_VOWEL_PEAK
                    stateStartTime = now
                }
            }
            State.I_VOWEL_PEAK -> {
                val elapsed = now - stateStartTime
                if (elapsed > 550) {
                    currentState = State.SEARCHING
                } else if (features.rms < baseThreshold * 1.2f && elapsed > 80) {
                    // Energy dropped smoothly after vowel peak, matching short rhotic "Mir" finish!
                    triggerWakeWord(now)
                }
            }
        }
    }

    private fun triggerWakeWord(now: Long) {
        lastTriggerTime = now
        currentState = State.SEARCHING
        frameHistory.clear()
        onWakeWordDetected()
    }

    private fun extractFeatures(bytes: ByteArray, sampleRate: Int, now: Long): FrameFeatures {
        val sampleCount = bytes.size / 2
        if (sampleCount == 0) return FrameFeatures(0f, 0f, 0f, now)

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var sumSquares = 0.0
        var zeroCrossings = 0
        var prevSample = 0

        // Simple low-pass filter accumulator to approximate low-frequency energy (100-800Hz)
        var lowFilterSum = 0.0
        var prevFiltered = 0.0
        val alpha = 0.25 // RC filter parameter

        for (i in 0 until sampleCount) {
            val sample = buffer.short.toInt()
            sumSquares += sample * sample

            if ((sample > 0 && prevSample <= 0) || (sample < 0 && prevSample >= 0)) {
                zeroCrossings++
            }

            // Simple 1st-order IIR lowpass
            val filtered = alpha * sample + (1 - alpha) * prevFiltered
            lowFilterSum += filtered * filtered
            prevFiltered = filtered

            prevSample = sample
        }

        val rms = (sqrt(sumSquares / sampleCount) / 32767.0).toFloat()
        val zcr = zeroCrossings.toFloat() / sampleCount.toFloat()
        val lowRms = (sqrt(lowFilterSum / sampleCount) / 32767.0).toFloat()
        val lowFreqRatio = if (rms > 0.0001f) (lowRms / rms).coerceIn(0f, 1f) else 0f

        return FrameFeatures(
            rms = rms,
            zcr = zcr,
            lowFreqRatio = lowFreqRatio,
            timestamp = now
        )
    }

    fun reset() {
        currentState = State.SEARCHING
        frameHistory.clear()
    }
}
