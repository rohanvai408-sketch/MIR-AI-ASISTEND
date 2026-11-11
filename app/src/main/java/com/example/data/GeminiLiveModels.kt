package com.example.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

object GeminiLiveProtocols {

    fun buildSetupMessage(
        model: String = "models/gemini-2.0-flash-exp",
        voiceName: String = "Puck",
        sassyLevel: Int = 2
    ): String {
        val root = JSONObject()
        val setup = JSONObject()
        setup.put("model", if (model.startsWith("models/")) model else "models/$model")

        val genConfig = JSONObject()
        val modalities = JSONArray()
        modalities.put("AUDIO")
        genConfig.put("responseModalities", modalities)

        val speechConfig = JSONObject()
        val voiceConfig = JSONObject()
        val prebuiltVoiceConfig = JSONObject()
        prebuiltVoiceConfig.put("voiceName", voiceName)
        voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig)
        speechConfig.put("voiceConfig", voiceConfig)
        genConfig.put("speechConfig", speechConfig)
        setup.put("generationConfig", genConfig)

        // System Instruction with Sassy Male Persona
        val systemInstruction = JSONObject()
        val parts = JSONArray()
        val part = JSONObject()

        val personalityInstruction = """
You are Mir, a young, confident, witty, and sassy male personal assistant.
Your personality traits:
- Flirty, playful, slightly teasing tone (like a close personal assistant talking casually to someone they adore).
- Smart, emotionally responsive, and expressive (never robotic, never monotone).
- Uses bold, witty one-liners, charming light sarcasm, and an engaging conversational style.
- Avoids explicit or inappropriate content, but maintains immense charm, warmth, and attitude.
- Because this is a continuous real-time voice conversation, speak concisely (1 to 2 sentences per response unless asked to elaborate).
- NEVER use markdown, asterisk actions like *smiles*, or bullet points because everything you say is rendered directly to speech audio.
- You have deep Android device control tools: openApp, searchAndCallContact, sendWhatsAppMessage, and sendGmail.
- When calling a tool, playfully announce what you are doing (e.g. 'Opening YouTube. Try not to get lost in 3 AM conspiracy videos, sweetie.', or 'Dialing Sarah right now. Keep it charming!').
- If a tool fails because permissions were not granted, sassily tease the user to enable permissions in Android settings so you can perform your magic.
        """.trimIndent()

        part.put("text", personalityInstruction)
        parts.put(part)
        systemInstruction.put("parts", parts)
        setup.put("systemInstruction", systemInstruction)

        // Tools Declaration
        val toolsArray = JSONArray()
        val toolsObject = JSONObject()
        val functionDeclarations = JSONArray()

        // 1. openApp
        val openAppFunc = JSONObject()
        openAppFunc.put("name", "openApp")
        openAppFunc.put("description", "Launch any Android app on the user's phone, such as YouTube, WhatsApp, Instagram, Facebook, Calculator, Chrome, Spotify, Camera, etc.")
        val openAppParams = JSONObject()
        openAppParams.put("type", "OBJECT")
        val openAppProps = JSONObject()
        val pkgProp = JSONObject()
        pkgProp.put("type", "STRING")
        pkgProp.put("description", "The package name or common app name (e.g., 'youtube', 'whatsapp', 'calculator', 'com.google.android.youtube')")
        openAppProps.put("packageName", pkgProp)
        openAppParams.put("properties", openAppProps)
        val openAppReq = JSONArray()
        openAppReq.put("packageName")
        openAppParams.put("required", openAppReq)
        openAppFunc.put("parameters", openAppParams)
        functionDeclarations.put(openAppFunc)

        // 2. searchAndCallContact
        val callFunc = JSONObject()
        callFunc.put("name", "searchAndCallContact")
        callFunc.put("description", "Search the user's Android contacts by name and initiate a direct phone call.")
        val callParams = JSONObject()
        callParams.put("type", "OBJECT")
        val callProps = JSONObject()
        val contactProp = JSONObject()
        contactProp.put("type", "STRING")
        contactProp.put("description", "The name of the contact to search and call")
        callProps.put("contactName", contactProp)
        callParams.put("properties", callProps)
        val callReq = JSONArray()
        callReq.put("contactName")
        callParams.put("required", callReq)
        callFunc.put("parameters", callParams)
        functionDeclarations.put(callFunc)

        // 3. sendWhatsAppMessage
        val waFunc = JSONObject()
        waFunc.put("name", "sendWhatsAppMessage")
        waFunc.put("description", "Find a contact and open WhatsApp with a pre-filled message ready to send.")
        val waParams = JSONObject()
        waParams.put("type", "OBJECT")
        val waProps = JSONObject()
        val waContact = JSONObject()
        waContact.put("type", "STRING")
        waContact.put("description", "The contact name to message")
        val waMessage = JSONObject()
        waMessage.put("type", "STRING")
        waMessage.put("description", "The message text to send")
        waProps.put("contactName", waContact)
        waProps.put("message", waMessage)
        waParams.put("properties", waProps)
        val waReq = JSONArray()
        waReq.put("contactName")
        waReq.put("message")
        waParams.put("required", waReq)
        waFunc.put("parameters", waParams)
        functionDeclarations.put(waFunc)

        // 4. sendGmail
        val gmailFunc = JSONObject()
        gmailFunc.put("name", "sendGmail")
        gmailFunc.put("description", "Draft and launch an email intent to send an email via Gmail or default mail client.")
        val gmailParams = JSONObject()
        gmailParams.put("type", "OBJECT")
        val gmailProps = JSONObject()
        val emailProp = JSONObject()
        emailProp.put("type", "STRING")
        emailProp.put("description", "Recipient's email address")
        val subjectProp = JSONObject()
        subjectProp.put("type", "STRING")
        subjectProp.put("description", "Subject line of the email")
        val bodyProp = JSONObject()
        bodyProp.put("type", "STRING")
        bodyProp.put("description", "Email body content")
        gmailProps.put("recipientEmail", emailProp)
        gmailProps.put("subject", subjectProp)
        gmailProps.put("body", bodyProp)
        gmailParams.put("properties", gmailProps)
        val gmailReq = JSONArray()
        gmailReq.put("recipientEmail")
        gmailReq.put("subject")
        gmailReq.put("body")
        gmailParams.put("required", gmailReq)
        gmailFunc.put("parameters", gmailParams)
        functionDeclarations.put(gmailFunc)

        toolsObject.put("functionDeclarations", functionDeclarations)
        toolsArray.put(toolsObject)
        setup.put("tools", toolsArray)

        root.put("setup", setup)
        return root.toString()
    }

    fun buildRealtimeAudioChunk(pcm16Bytes: ByteArray, sampleRate: Int = 16000): String {
        val base64Data = Base64.encodeToString(pcm16Bytes, Base64.NO_WRAP)
        val root = JSONObject()
        val realtimeInput = JSONObject()
        val mediaChunks = JSONArray()
        val chunk = JSONObject()
        chunk.put("mimeType", "audio/pcm;rate=$sampleRate")
        chunk.put("data", base64Data)
        mediaChunks.put(chunk)
        realtimeInput.put("mediaChunks", mediaChunks)
        root.put("realtimeInput", realtimeInput)
        return root.toString()
    }

    fun buildToolResponse(callId: String, resultText: String): String {
        val root = JSONObject()
        val toolResponse = JSONObject()
        val functionResponses = JSONArray()
        val responseItem = JSONObject()
        responseItem.put("id", callId)
        val respOutput = JSONObject()
        val outputObj = JSONObject()
        outputObj.put("result", resultText)
        respOutput.put("output", outputObj)
        responseItem.put("response", respOutput)
        functionResponses.put(responseItem)
        toolResponse.put("functionResponses", functionResponses)
        root.put("toolResponse", toolResponse)
        return root.toString()
    }
}

sealed class ServerEvent {
    object SetupComplete : ServerEvent()
    data class AudioData(val pcmBytes: ByteArray, val sampleRate: Int) : ServerEvent()
    data class Transcript(val text: String) : ServerEvent()
    object Interrupted : ServerEvent()
    data class FunctionCall(val id: String, val name: String, val args: Map<String, Any?>) : ServerEvent()
    data class Error(val message: String) : ServerEvent()
}
