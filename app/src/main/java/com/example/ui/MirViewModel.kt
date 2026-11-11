package com.example.ui

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlaybackManager
import com.example.audio.AudioRecordManager
import com.example.audio.WakeWordDetector
import com.example.data.MirPreferences
import com.example.data.ToolExecutionEngine
import com.example.service.MirAudioForegroundService
import com.example.session.AssistantState
import com.example.session.LiveSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MirViewModel(application: Application) : AndroidViewModel(application) {

    val preferences = MirPreferences(application)
    private val toolEngine = ToolExecutionEngine(application)
    private val audioPlaybackManager = AudioPlaybackManager()

    private val wakeWordDetector = WakeWordDetector(
        sensitivity = preferences.wakeWordSensitivity,
        isEnabled = preferences.isWakeWordEnabled,
        onWakeWordDetected = {
            onWakeWordDetected()
        }
    )

    private val audioRecordManager = AudioRecordManager(wakeWordDetector) { chunk ->
        liveSessionManager.sendAudioChunk(chunk)
    }

    val liveSessionManager = LiveSessionManager(
        context = application,
        preferences = preferences,
        audioPlaybackManager = audioPlaybackManager,
        toolEngine = toolEngine
    )

    // Permission states
    private val _hasAudioPermission = MutableStateFlow(false)
    val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()

    private val _hasContactsPermission = MutableStateFlow(false)
    val hasContactsPermission: StateFlow<Boolean> = _hasContactsPermission.asStateFlow()

    private val _hasCallPermission = MutableStateFlow(false)
    val hasCallPermission: StateFlow<Boolean> = _hasCallPermission.asStateFlow()

    private val _hasNotificationPermission = MutableStateFlow(false)
    val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission.asStateFlow()

    // Onboarding status
    private val _isOnboardingNeeded = MutableStateFlow(!preferences.isOnboardingCompleted)
    val isOnboardingNeeded: StateFlow<Boolean> = _isOnboardingNeeded.asStateFlow()

    // Settings dialog
    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    // Help dialog
    private val _isHelpOpen = MutableStateFlow(false)
    val isHelpOpen: StateFlow<Boolean> = _isHelpOpen.asStateFlow()

    // Bound Service reference (if running)
    private var boundService: MirAudioForegroundService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? MirAudioForegroundService.LocalBinder
            boundService = localBinder?.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
            isServiceBound = false
        }
    }

    val assistantState: StateFlow<AssistantState> = liveSessionManager.assistantState
    val subtitleText: StateFlow<String> = liveSessionManager.subtitleText
    val lastToolAction: StateFlow<String?> = liveSessionManager.lastToolAction
    val connectionStatus: StateFlow<String> = liveSessionManager.connectionStatus

    val inputAmplitude: StateFlow<Float> = audioRecordManager.inputAmplitude
    val outputAmplitude: StateFlow<Float> = audioPlaybackManager.outputAmplitude

    init {
        checkPermissions()
        if (preferences.isBackgroundServiceRunning) {
            bindForegroundService()
        }
    }

    fun checkPermissions() {
        val app = getApplication<Application>()
        _hasAudioPermission.value = ContextCompat.checkSelfPermission(
            app,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        _hasContactsPermission.value = ContextCompat.checkSelfPermission(
            app,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        _hasCallPermission.value = ContextCompat.checkSelfPermission(
            app,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        _hasNotificationPermission.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                app,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        // If audio permission is not granted, enforce onboarding
        if (!_hasAudioPermission.value) {
            _isOnboardingNeeded.value = true
        }
    }

    fun completeOnboarding() {
        preferences.isOnboardingCompleted = true
        _isOnboardingNeeded.value = false
        startAssistant()
    }

    fun startAssistant() {
        checkPermissions()
        if (!_hasAudioPermission.value) {
            _isOnboardingNeeded.value = true
            return
        }

        audioRecordManager.startRecording()
        audioRecordManager.streamToGemini = true
        liveSessionManager.startSession()

        if (preferences.isBackgroundServiceRunning) {
            MirAudioForegroundService.startService(getApplication())
            bindForegroundService()
        }
    }

    fun onOrbClicked() {
        checkPermissions()
        if (!_hasAudioPermission.value) {
            _isOnboardingNeeded.value = true
            return
        }

        when (assistantState.value) {
            AssistantState.SPEAKING -> {
                // Interruption: instantly stop playback and start listening
                audioPlaybackManager.interrupt()
                liveSessionManager.onUserInitiatedWake()
            }
            AssistantState.IDLE, AssistantState.ERROR -> {
                audioRecordManager.startRecording()
                audioRecordManager.streamToGemini = true
                liveSessionManager.startSession()
                liveSessionManager.onUserInitiatedWake()
            }
            AssistantState.LISTENING -> {
                // Re-awaken / focus
                liveSessionManager.onUserInitiatedWake()
            }
            AssistantState.THINKING, AssistantState.EXECUTING_TOOL -> {
                // Interrupt ongoing thinking
                audioPlaybackManager.interrupt()
                liveSessionManager.onUserInitiatedWake()
            }
        }
    }

    private fun onWakeWordDetected() {
        audioRecordManager.streamToGemini = true
        liveSessionManager.onUserInitiatedWake()
    }

    fun executeManualTool(toolName: String, args: Map<String, Any?>) {
        liveSessionManager.executeManualTool(toolName, args)
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun openHelp() {
        _isHelpOpen.value = true
    }

    fun closeHelp() {
        _isHelpOpen.value = false
    }

    fun toggleBackgroundService(enable: Boolean) {
        preferences.isBackgroundServiceRunning = enable
        val context = getApplication<Application>()
        if (enable) {
            MirAudioForegroundService.startService(context)
            bindForegroundService()
        } else {
            unbindForegroundService()
            MirAudioForegroundService.stopService(context)
        }
    }

    private fun bindForegroundService() {
        if (!isServiceBound) {
            val intent = Intent(getApplication(), MirAudioForegroundService::class.java)
            getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindForegroundService() {
        if (isServiceBound) {
            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (e: Exception) {
                // Ignore unbind issues
            }
            isServiceBound = false
            boundService = null
        }
    }

    fun reloadPreferencesAndRestartSession() {
        wakeWordDetector.isEnabled = preferences.isWakeWordEnabled
        wakeWordDetector.sensitivity = preferences.wakeWordSensitivity

        if (preferences.isBackgroundServiceRunning) {
            toggleBackgroundService(true)
        } else {
            toggleBackgroundService(false)
        }

        liveSessionManager.startSession()
        closeSettings()
    }

    override fun onCleared() {
        super.onCleared()
        unbindForegroundService()
        audioRecordManager.stopRecording()
        audioPlaybackManager.release()
        liveSessionManager.release()
    }
}
