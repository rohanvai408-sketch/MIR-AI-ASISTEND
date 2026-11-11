package com.example.session

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.Log
import com.example.audio.AudioPlaybackManager
import com.example.data.GeminiLiveProtocols
import com.example.data.MirPreferences
import com.example.data.ToolExecutionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

class LiveSessionManager(
    private val context: Context,
    private val preferences: MirPreferences,
    private val audioPlaybackManager: AudioPlaybackManager,
    private val toolEngine: ToolExecutionEngine
) {
    companion object {
        private const val TAG = "LiveSessionManager"
        private const val BASE_LIVE_URL =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .pingInterval(0, TimeUnit.MILLISECONDS) // Disabled: Gemini Live backend does not echo client pings, avoiding SocketTimeoutException
        .retryOnConnectionFailure(true)
        .build()

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _subtitleText = MutableStateFlow("")
    val subtitleText: StateFlow<String> = _subtitleText.asStateFlow()

    private val _lastToolAction = MutableStateFlow<String?>(null)
    val lastToolAction: StateFlow<String?> = _lastToolAction.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    // Offline / Preview Fallback TTS when API Key is missing or user requests quick test
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                textToSpeech?.setPitch(1.05f) // Confident, young male resonance
                textToSpeech?.setSpeechRate(1.1f) // Energetic and witty
                isTtsReady = true
            }
        }
    }

    fun startSession() {
        val apiKey = preferences.effectiveApiKey
        if (apiKey.isBlank()) {
            _connectionStatus.value = "Demo Mode (API Key not set)"
            _assistantState.value = AssistantState.LISTENING
            _subtitleText.value = "Hey handsome. Add your Gemini API key in Settings for full Live audio, or give me a command now!"
            speakSassyFallback("Hey darling. I am ready. You can test device commands or add your Gemini API key in settings for live audio streaming.")
            return
        }

        connectWebSocket(apiKey)
    }

    private fun connectWebSocket(apiKey: String) {
        disconnect()

        _connectionStatus.value = "Connecting to Gemini Live..."
        _assistantState.value = AssistantState.THINKING

        val url = "$BASE_LIVE_URL?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Gemini Live WebSocket opened!")
                _connectionStatus.value = "Connected (Live Session)"
                
                // Send Setup Message
                val setupJson = GeminiLiveProtocols.buildSetupMessage(
                    model = preferences.selectedModel,
                    voiceName = preferences.selectedVoice,
                    sassyLevel = preferences.sassyLevel
                )
                webSocket.send(setupJson)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}, response code: ${response?.code}", t)
                val code = response?.code
                val reason = when (code) {
                    400 -> "Invalid setup parameter or model version"
                    403 -> "Invalid or unauthorized API key"
                    404 -> "Model endpoint not found"
                    429 -> "Rate limit reached"
                    else -> t.localizedMessage ?: "Network error"
                }
                _connectionStatus.value = "Live audio error: $reason"
                _assistantState.value = AssistantState.ERROR
                _subtitleText.value = "Mir: Live connection interrupted ($reason). Tap orb or use quick actions, darling."
                this@LiveSessionManager.webSocket = null
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                _connectionStatus.value = "Disconnected ($reason)"
                _assistantState.value = AssistantState.IDLE
                this@LiveSessionManager.webSocket = null
            }
        })
    }

    private fun handleServerMessage(jsonString: String) {
        try {
            val root = JSONObject(jsonString)

            // 1. Setup complete
            if (root.has("setupComplete")) {
                Log.d(TAG, "Gemini Live setup complete!")
                _assistantState.value = AssistantState.LISTENING
                _subtitleText.value = "Mir is awake and ready for you, gorgeous."
                return
            }

            // 2. Server Content (Audio / Text / Interrupted)
            if (root.has("serverContent")) {
                val serverContent = root.getJSONObject("serverContent")

                if (serverContent.optBoolean("interrupted", false)) {
                    Log.d(TAG, "Gemini Live detected user interruption!")
                    audioPlaybackManager.interrupt()
                    _assistantState.value = AssistantState.LISTENING
                    _subtitleText.value = "You spoke? I'm listening!"
                    return
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)

                            // Text transcript
                            if (part.has("text")) {
                                val transcript = part.getString("text")
                                _subtitleText.value = transcript
                            }

                            // Audio chunk
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val base64Audio = inlineData.optString("data", "")
                                if (base64Audio.isNotBlank()) {
                                    val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
                                    _assistantState.value = AssistantState.SPEAKING
                                    audioPlaybackManager.enqueueAudio(audioBytes)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Tool Calls from Gemini
            if (root.has("toolCall")) {
                val toolCall = root.getJSONObject("toolCall")
                val functionCalls = toolCall.optJSONArray("functionCalls")
                if (functionCalls != null) {
                    _assistantState.value = AssistantState.EXECUTING_TOOL
                    for (i in 0 until functionCalls.length()) {
                        val call = functionCalls.getJSONObject(i)
                        val callId = call.getString("id")
                        val funcName = call.getString("name")
                        val argsObj = call.optJSONObject("args")
                        val argsMap = mutableMapOf<String, Any?>()
                        if (argsObj != null) {
                            val keys = argsObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                argsMap[key] = argsObj.opt(key)
                            }
                        }

                        _lastToolAction.value = "Executing $funcName..."
                        executeToolAndRespond(callId, funcName, argsMap)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server message: ${e.message}", e)
        }
    }

    private fun executeToolAndRespond(callId: String, toolName: String, args: Map<String, Any?>) {
        scope.launch {
            val result = toolEngine.execute(toolName, args)
            _lastToolAction.value = result.userFriendlyMessage
            _subtitleText.value = result.userFriendlyMessage

            // Send tool response back to Gemini Live
            val responseJson = GeminiLiveProtocols.buildToolResponse(
                callId = callId,
                resultText = "${result.userFriendlyMessage} (${result.technicalDetails})"
            )
            webSocket?.send(responseJson)
        }
    }

    /**
     * Send microphone PCM16 audio chunk into the Gemini Live WebSocket.
     */
    fun sendAudioChunk(pcm16Bytes: ByteArray) {
        val ws = webSocket
        if (ws != null) {
            val chunkMessage = GeminiLiveProtocols.buildRealtimeAudioChunk(pcm16Bytes)
            ws.send(chunkMessage)
        }
    }

    /**
     * Triggered when the user taps the Orb or wake-word is detected.
     */
    fun onUserInitiatedWake() {
        audioPlaybackManager.interrupt()
        _assistantState.value = AssistantState.LISTENING

        val apiKey = preferences.effectiveApiKey
        if (apiKey.isBlank() || webSocket == null) {
            _subtitleText.value = "Yes, darling? What can Mir do for you?"
            speakSassyFallback("Yes darling, what can Mir do for you?")
        } else {
            _subtitleText.value = "Listening to you, darling..."
        }
    }

    fun stopSession() {
        audioPlaybackManager.interrupt()
        _assistantState.value = AssistantState.IDLE
        _subtitleText.value = "Mir is resting. Say 'Mir' anytime to wake me up."
    }

    fun executeManualTool(toolName: String, args: Map<String, Any?>) {
        audioPlaybackManager.interrupt()
        _assistantState.value = AssistantState.EXECUTING_TOOL
        scope.launch {
            val result = toolEngine.execute(toolName, args)
            _lastToolAction.value = result.userFriendlyMessage
            _subtitleText.value = result.userFriendlyMessage
            speakSassyFallback(result.userFriendlyMessage)
            delay(2000)
            if (_assistantState.value == AssistantState.EXECUTING_TOOL) {
                _assistantState.value = AssistantState.IDLE
            }
        }
    }

    private fun speakSassyFallback(message: String) {
        if (isTtsReady) {
            _assistantState.value = AssistantState.SPEAKING
            textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "mir_sassy_utterance")
            scope.launch {
                delay(2200)
                if (_assistantState.value == AssistantState.SPEAKING) {
                    _assistantState.value = AssistantState.IDLE
                }
            }
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "User closed session")
        webSocket = null
        _connectionStatus.value = "Disconnected"
    }

    fun release() {
        disconnect()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
