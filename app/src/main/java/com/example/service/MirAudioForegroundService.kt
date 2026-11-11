package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.AudioPlaybackManager
import com.example.audio.AudioRecordManager
import com.example.audio.WakeWordDetector
import com.example.data.MirPreferences
import com.example.data.ToolExecutionEngine
import com.example.session.LiveSessionManager

class MirAudioForegroundService : Service() {

    companion object {
        private const val TAG = "MirForegroundService"
        private const val NOTIFICATION_CHANNEL_ID = "mir_voice_service_channel"
        private const val NOTIFICATION_ID = 40401

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_WAKE = "com.example.service.ACTION_WAKE"

        fun startService(context: Context) {
            val intent = Intent(context, MirAudioForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MirAudioForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private val binder = LocalBinder()

    lateinit var preferences: MirPreferences
        private set
    lateinit var audioPlaybackManager: AudioPlaybackManager
        private set
    lateinit var toolEngine: ToolExecutionEngine
        private set
    lateinit var liveSessionManager: LiveSessionManager
        private set
    lateinit var wakeWordDetector: WakeWordDetector
        private set
    lateinit var audioRecordManager: AudioRecordManager
        private set

    inner class LocalBinder : Binder() {
        fun getService(): MirAudioForegroundService = this@MirAudioForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MirAudioForegroundService onCreate")

        preferences = MirPreferences(this)
        toolEngine = ToolExecutionEngine(this)
        audioPlaybackManager = AudioPlaybackManager()
        liveSessionManager = LiveSessionManager(this, preferences, audioPlaybackManager, toolEngine)

        wakeWordDetector = WakeWordDetector(
            sensitivity = preferences.wakeWordSensitivity,
            isEnabled = preferences.isWakeWordEnabled,
            onWakeWordDetected = {
                handleWakeWordTriggered()
            }
        )

        audioRecordManager = AudioRecordManager(wakeWordDetector) { pcmBytes ->
            liveSessionManager.sendAudioChunk(pcmBytes)
        }

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_WAKE -> {
                handleWakeWordTriggered()
            }
            else -> {
                startInForeground()
                startBackgroundListening()
            }
        }
        return START_STICKY
    }

    private fun startInForeground() {
        val notification = buildForegroundNotification("Mir is listening for 'Mir'...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        preferences.isBackgroundServiceRunning = true
    }

    private fun buildForegroundNotification(statusText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val wakeIntent = Intent(this, MirAudioForegroundService::class.java).apply {
            action = ACTION_WAKE
        }
        val pendingWake = PendingIntent.getService(
            this,
            1,
            wakeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, MirAudioForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Mir Voice Assistant")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.mir_app_icon)
            .setOngoing(true)
            .setContentIntent(pendingOpen)
            .addAction(0, "Awaken Mir", pendingWake)
            .addAction(0, "Stop Service", pendingStop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startBackgroundListening() {
        val recordingStarted = audioRecordManager.startRecording()
        if (recordingStarted) {
            Log.d(TAG, "Audio recording started in background service")
        }
    }

    private fun handleWakeWordTriggered() {
        Log.d(TAG, "Wake-word 'Mir' detected!")

        // Haptic feedback
        vibrateOnWake()

        // Awaken Live Session
        audioRecordManager.streamToGemini = true
        liveSessionManager.onUserInitiatedWake()

        // Update notification
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildForegroundNotification("Mir awakened! Listening to you..."))

        // Bring activity to front if user is in background
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(launchIntent)
    }

    private fun vibrateOnWake() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vm.defaultVibrator
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(120)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Mir Voice Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground microphone monitoring and wake-word detection for Mir"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun stopForegroundService() {
        preferences.isBackgroundServiceRunning = false
        audioRecordManager.stopRecording()
        audioPlaybackManager.release()
        liveSessionManager.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        preferences.isBackgroundServiceRunning = false
        audioRecordManager.stopRecording()
        audioPlaybackManager.release()
        liveSessionManager.release()
    }
}
