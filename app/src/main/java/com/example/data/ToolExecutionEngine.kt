package com.example.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

class ToolExecutionEngine(private val context: Context) {

    data class ExecutionResult(
        val success: Boolean,
        val userFriendlyMessage: String,
        val technicalDetails: String,
        val missingPermission: String? = null
    )

    fun execute(toolName: String, args: Map<String, Any?>): ExecutionResult {
        return when (toolName) {
            "openApp" -> {
                val pkgOrName = args["packageName"]?.toString() ?: ""
                openApp(pkgOrName)
            }
            "searchAndCallContact" -> {
                val contactName = args["contactName"]?.toString() ?: ""
                searchAndCallContact(contactName)
            }
            "sendWhatsAppMessage" -> {
                val contactName = args["contactName"]?.toString() ?: ""
                val message = args["message"]?.toString() ?: ""
                sendWhatsAppMessage(contactName, message)
            }
            "sendGmail" -> {
                val recipient = args["recipientEmail"]?.toString() ?: ""
                val subject = args["subject"]?.toString() ?: ""
                val body = args["body"]?.toString() ?: ""
                sendGmail(recipient, subject, body)
            }
            else -> {
                ExecutionResult(
                    success = false,
                    userFriendlyMessage = "I don't know that trick yet, darling.",
                    technicalDetails = "Unknown tool: $toolName"
                )
            }
        }
    }

    private fun openApp(query: String): ExecutionResult {
        val trimmed = query.trim().lowercase()
        val pm = context.packageManager

        // Check common aliases
        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "facebook" to "com.facebook.katana",
            "calculator" to "com.google.android.calculator",
            "gmail" to "com.google.android.gm",
            "chrome" to "com.android.chrome",
            "spotify" to "com.spotify.music",
            "maps" to "com.google.android.apps.maps",
            "twitter" to "com.twitter.android",
            "x" to "com.twitter.android"
        )

        val targetPackage = knownPackages[trimmed] ?: trimmed

        // Try direct launch intent
        val directIntent = pm.getLaunchIntentForPackage(targetPackage)
        if (directIntent != null) {
            directIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(directIntent)
            return ExecutionResult(
                success = true,
                userFriendlyMessage = "Launched $query seamlessly.",
                technicalDetails = "Launched package: $targetPackage"
            )
        }

        // Try category / intent matching (e.g. calculator, browser, camera)
        if (trimmed.contains("calc")) {
            val calcIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_CALCULATOR)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (calcIntent.resolveActivity(pm) != null) {
                context.startActivity(calcIntent)
                return ExecutionResult(
                    success = true,
                    userFriendlyMessage = "Opened Calculator for you.",
                    technicalDetails = "Launched CATEGORY_APP_CALCULATOR"
                )
            }
        }

        // Try search installed apps by label
        try {
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in installed) {
                val label = pm.getApplicationLabel(appInfo).toString().lowercase()
                if (label.contains(trimmed) || trimmed.contains(label)) {
                    val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return ExecutionResult(
                            success = true,
                            userFriendlyMessage = "Found and opened ${pm.getApplicationLabel(appInfo)}.",
                            technicalDetails = "Launched ${appInfo.packageName}"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore reflection / query limits
        }

        return ExecutionResult(
            success = false,
            userFriendlyMessage = "Couldn't locate $query on your device. Is it installed?",
            technicalDetails = "No package or launcher intent found for $query"
        )
    }

    private fun searchAndCallContact(contactName: String): ExecutionResult {
        val hasContactsPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasContactsPerm) {
            return ExecutionResult(
                success = false,
                userFriendlyMessage = "I need your permission to access Contacts first. Grant me contacts access in settings, darling!",
                technicalDetails = "READ_CONTACTS permission not granted",
                missingPermission = Manifest.permission.READ_CONTACTS
            )
        }

        val (matchedName, matchedNumber) = queryContact(contactName)
            ?: return ExecutionResult(
                success = false,
                userFriendlyMessage = "Couldn't find anyone named '$contactName' in your address book.",
                technicalDetails = "Contact lookup returned 0 results"
            )

        val hasCallPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return if (hasCallPerm) {
            val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(matchedNumber)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(callIntent)
            ExecutionResult(
                success = true,
                userFriendlyMessage = "Calling $matchedName now. Don't be shy!",
                technicalDetails = "ACTION_CALL triggered for $matchedName ($matchedNumber)"
            )
        } else {
            // Fallback to dialer if direct call permission is missing
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(matchedNumber)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            ExecutionResult(
                success = true,
                userFriendlyMessage = "Opened the phone dialer for $matchedName ($matchedNumber). Grant CALL_PHONE permission if you want me to call directly without tapping call!",
                technicalDetails = "ACTION_DIAL fallback used because CALL_PHONE is not granted",
                missingPermission = Manifest.permission.CALL_PHONE
            )
        }
    }

    private fun sendWhatsAppMessage(contactName: String, message: String): ExecutionResult {
        var phoneNumber: String? = null
        var foundName = contactName

        val hasContactsPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasContactsPerm) {
            val contact = queryContact(contactName)
            if (contact != null) {
                foundName = contact.first
                phoneNumber = contact.second.replace("[^0-9+]".toRegex(), "")
            }
        }

        // Deep-link intent into WhatsApp
        val uri = if (!phoneNumber.isNullOrBlank()) {
            Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}")
        } else {
            Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
        }

        val waIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pm = context.packageManager
        if (waIntent.resolveActivity(pm) != null) {
            context.startActivity(waIntent)
            return ExecutionResult(
                success = true,
                userFriendlyMessage = "WhatsApp ready with your message to $foundName.",
                technicalDetails = "Deep link into WhatsApp opened with text"
            )
        }

        // Generic share fallback
        val genericIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(genericIntent)
            ExecutionResult(
                success = true,
                userFriendlyMessage = "Opened WhatsApp link for $foundName.",
                technicalDetails = "Generic VIEW intent for WhatsApp link"
            )
        } catch (e: Exception) {
            ExecutionResult(
                success = false,
                userFriendlyMessage = "WhatsApp isn't installed on this phone, darling.",
                technicalDetails = "Failed to launch WhatsApp intent: ${e.message}"
            )
        }
    }

    private fun sendGmail(recipientEmail: String, subject: String, body: String): ExecutionResult {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${Uri.encode(recipientEmail)}")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pm = context.packageManager
        return if (emailIntent.resolveActivity(pm) != null) {
            context.startActivity(emailIntent)
            ExecutionResult(
                success = true,
                userFriendlyMessage = "Opened your email draft to $recipientEmail. Ready when you are!",
                technicalDetails = "ACTION_SENDTO intent dispatched with subject: '$subject'"
            )
        } else {
            ExecutionResult(
                success = false,
                userFriendlyMessage = "No email application found to send this mail.",
                technicalDetails = "No activity found to handle ACTION_SENDTO"
            )
        }
    }

    private fun queryContact(contactName: String): Pair<String, String>? {
        val cr = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$contactName%")

        cr.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else contactName
                val number = if (numberIndex >= 0) cursor.getString(numberIndex) else ""
                if (number.isNotBlank()) {
                    return Pair(name, number)
                }
            }
        }
        return null
    }
}
