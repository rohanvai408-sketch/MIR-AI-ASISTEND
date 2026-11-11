package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.MirPreferences
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.SassyAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun MirSettingsDialog(
    preferences: MirPreferences,
    onDismiss: () -> Unit,
    onSaveAndRestartSession: () -> Unit
) {
    var apiKeyText by remember { mutableStateOf(preferences.customApiKey) }
    var selectedVoice by remember { mutableStateOf(preferences.selectedVoice) }
    var selectedModel by remember { mutableStateOf(preferences.selectedModel) }
    var isWakeWordEnabled by remember { mutableStateOf(preferences.isWakeWordEnabled) }
    var sensitivity by remember { mutableFloatStateOf(preferences.wakeWordSensitivity) }
    var isBgServiceEnabled by remember { mutableStateOf(preferences.isBackgroundServiceRunning) }
    var sassyLevel by remember { mutableIntStateOf(preferences.sassyLevel) }

    data class VoiceOption(
        val name: String,
        val gender: String,
        val descriptor: String,
        val tone: String
    )

    val voiceOptions = listOf(
        VoiceOption("Puck", "Male", "Charismatic & Witty", "Youthful, energetic cadence"),
        VoiceOption("Charon", "Male", "Deep Baritone & Executive", "Calm, deep authoritative resonance"),
        VoiceOption("Fenrir", "Male", "Smooth & Grounded", "Articulate, balanced professional voice"),
        VoiceOption("Aoede", "Female", "Warm & Melodious", "Expressive, sophisticated female tone"),
        VoiceOption("Kore", "Female", "Calm & Elegant Concierge", "Clear, soothing modern voice")
    )

    val models = listOf(
        "gemini-2.0-flash-exp",
        "gemini-2.5-flash-native-audio-preview",
        "gemini-3.1-flash-live-preview"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ObsidianDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Mir Preferences",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("settings_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 1: API Key
                Text(
                    text = "GEMINI LIVE API KEY",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    placeholder = {
                        Text(
                            text = if (preferences.effectiveApiKey.isNotBlank()) "Loaded from BuildConfig / Secrets" else "Enter Gemini API Key",
                            fontSize = 13.sp,
                            color = TextTertiary
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = ObsidianSurfaceContainer,
                        focusedContainerColor = ObsidianDark,
                        unfocusedContainerColor = ObsidianDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_api_key_input")
                )
                Text(
                    text = "Configured via AI Studio Secrets panel or paste directly here.",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Section 2: Voice Persona (Male & Female)
                Text(
                    text = "VOICE ENGINE & PERSONA",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Completely natural neural human voices with realistic cadence, zero robotic stiffness.",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                voiceOptions.forEach { voice ->
                    val isSelected = selectedVoice == voice.name
                    val genderColor = if (voice.gender == "Male") CyberCyan else CyberViolet

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) genderColor.copy(alpha = 0.18f) else ObsidianDark)
                            .border(
                                1.dp,
                                if (isSelected) genderColor else ObsidianSurfaceContainer,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedVoice = voice.name }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = voice.name,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(genderColor.copy(alpha = 0.25f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = voice.gender.uppercase(),
                                        color = genderColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${voice.descriptor} • ${voice.tone}",
                                color = if (isSelected) TextSecondary else TextTertiary,
                                fontSize = 11.sp
                            )
                        }

                        if (isSelected) {
                            Text(
                                text = "ACTIVE",
                                color = genderColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 3: Model Target
                Text(
                    text = "MODEL TARGET",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                models.forEach { model ->
                    val isSelected = selectedModel == model
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CyberViolet.copy(alpha = 0.2f) else ObsidianDark)
                            .border(
                                1.dp,
                                if (isSelected) CyberViolet else ObsidianSurfaceContainer,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedModel = model }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = model,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Text(
                                text = "ACTIVE",
                                color = CyberViolet,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Section 4: Wake-Word & Sensitivity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Wake-word 'Mir'",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Awakens assistant on hearing 'Mir'",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isWakeWordEnabled,
                        onCheckedChange = { isWakeWordEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonGreen,
                            checkedTrackColor = ObsidianSurfaceContainer,
                            uncheckedTrackColor = ObsidianDark
                        ),
                        modifier = Modifier.testTag("settings_wakeword_switch")
                    )
                }

                if (isWakeWordEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sensitivity: ${(sensitivity * 100).toInt()}%",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = sensitivity,
                        onValueChange = { sensitivity = it },
                        valueRange = 0.2f..0.95f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonGreen,
                            activeTrackColor = NeonGreen,
                            inactiveTrackColor = ObsidianDark
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 5: Background Foreground Service
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Background Wake Service",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Keeps listening even when screen is locked or in other apps",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isBgServiceEnabled,
                        onCheckedChange = { isBgServiceEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberCyan,
                            checkedTrackColor = ObsidianSurfaceContainer,
                            uncheckedTrackColor = ObsidianDark
                        ),
                        modifier = Modifier.testTag("settings_bg_service_switch")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = {
                        preferences.customApiKey = apiKeyText
                        preferences.selectedVoice = selectedVoice
                        preferences.selectedModel = selectedModel
                        preferences.isWakeWordEnabled = isWakeWordEnabled
                        preferences.wakeWordSensitivity = sensitivity
                        preferences.isBackgroundServiceRunning = isBgServiceEnabled
                        preferences.sassyLevel = sassyLevel
                        onSaveAndRestartSession()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = ObsidianDark
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("settings_save_btn")
                ) {
                    Text(
                        text = "Save & Apply Changes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
