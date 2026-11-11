package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class MirPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("mir_voice_assistant_prefs", Context.MODE_PRIVATE)

    var customApiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    val effectiveApiKey: String
        get() {
            val custom = customApiKey
            if (custom.isNotBlank()) return custom
            return try {
                val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
                val key = (field.get(null) as? String)?.trim() ?: ""
                if (key == "MY_GEMINI_API_KEY") "" else key
            } catch (e: Throwable) {
                ""
            }
        }

    var selectedVoice: String
        get() = prefs.getString(KEY_VOICE, "Puck") ?: "Puck"
        set(value) = prefs.edit().putString(KEY_VOICE, value).apply()

    var selectedModel: String
        get() = prefs.getString(KEY_MODEL, "gemini-2.0-flash-exp") ?: "gemini-2.0-flash-exp"
        set(value) = prefs.edit().putString(KEY_MODEL, value).apply()

    var isWakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, value).apply()

    var wakeWordSensitivity: Float
        get() = prefs.getFloat(KEY_WAKE_WORD_SENSITIVITY, 0.7f)
        set(value) = prefs.edit().putFloat(KEY_WAKE_WORD_SENSITIVITY, value).apply()

    var isBackgroundServiceRunning: Boolean
        get() = prefs.getBoolean(KEY_BG_SERVICE, false)
        set(value) = prefs.edit().putBoolean(KEY_BG_SERVICE, value).apply()

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    var sassyLevel: Int // 1 = Playful, 2 = Sassy, 3 = Savage
        get() = prefs.getInt(KEY_SASSY_LEVEL, 2)
        set(value) = prefs.edit().putInt(KEY_SASSY_LEVEL, value).apply()

    companion object {
        private const val KEY_API_KEY = "custom_gemini_api_key"
        private const val KEY_VOICE = "selected_voice"
        private const val KEY_MODEL = "selected_model"
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        private const val KEY_WAKE_WORD_SENSITIVITY = "wake_word_sensitivity"
        private const val KEY_BG_SERVICE = "bg_service_running"
        private const val KEY_ONBOARDING_DONE = "onboarding_completed"
        private const val KEY_SASSY_LEVEL = "sassy_personality_level"
    }
}
