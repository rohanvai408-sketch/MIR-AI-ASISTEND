package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.SassyAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun QuickActionRow(
    onTriggerTool: (toolName: String, args: Map<String, Any?>) -> Unit,
    lastToolAction: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Active Tool Action Banner
        AnimatedVisibility(
            visible = !lastToolAction.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianSurfaceContainer)
                    .border(1.dp, SassyAmber.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .testTag("tool_action_banner")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = SassyAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = lastToolAction ?: "",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2
                    )
                }
            }
        }

        // Voice Command Hint
        Text(
            text = "SPEAK FREELY OR TRY QUICK ACTIONS",
            color = TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Horizontal Quick Test Pills
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                QuickActionPill(
                    label = "Open YouTube",
                    icon = Icons.Default.PlayArrow,
                    accentColor = CyberCyan,
                    onClick = {
                        onTriggerTool("openApp", mapOf("packageName" to "youtube"))
                    }
                )
            }
            item {
                QuickActionPill(
                    label = "Call Contact",
                    icon = Icons.Default.Call,
                    accentColor = NeonGreen,
                    onClick = {
                        onTriggerTool("searchAndCallContact", mapOf("contactName" to "Mom"))
                    }
                )
            }
            item {
                QuickActionPill(
                    label = "WhatsApp",
                    icon = Icons.AutoMirrored.Filled.Send,
                    accentColor = CyberViolet,
                    onClick = {
                        onTriggerTool(
                            "sendWhatsAppMessage",
                            mapOf("contactName" to "Bestie", "message" to "Hey darling, Mir sent this!")
                        )
                    }
                )
            }
            item {
                QuickActionPill(
                    label = "Draft Gmail",
                    icon = Icons.Default.Email,
                    accentColor = SassyAmber,
                    onClick = {
                        onTriggerTool(
                            "sendGmail",
                            mapOf(
                                "recipientEmail" to "boss@example.com",
                                "subject" to "Mir Voice Assistant Status",
                                "body" to "Hey, Mir is running flawlessly on Android with Gemini Live."
                            )
                        )
                    }
                )
            }
            item {
                QuickActionPill(
                    label = "Calculator",
                    icon = Icons.Default.SmartToy,
                    accentColor = CyberCyan,
                    onClick = {
                        onTriggerTool("openApp", mapOf("packageName" to "calculator"))
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickActionPill(
    label: String,
    icon: ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ObsidianDark)
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
