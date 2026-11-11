package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun PermissionsOnboardingView(
    hasAudioPermission: Boolean,
    hasContactsPermission: Boolean,
    hasCallPermission: Boolean,
    hasNotificationPermission: Boolean,
    onPermissionRequested: () -> Unit,
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onPermissionRequested()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ObsidianDark,
                        Color(0xFF0B0E17),
                        ObsidianSurface
                    )
                )
            )
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(36.dp))

            // Glowing shield emblem
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(ObsidianSurfaceContainer)
                    .border(2.dp, CyberCyan.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Welcome to Mir",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "“I need a few permissions to give you that five-star, lightning-fast personal assistant treatment. Don't leave me stranded, darling.”",
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Permission Items
            PermissionCard(
                title = "Microphone Access",
                subtitle = "Mandatory. Mir needs his ears to hear your voice and wake-word in real time.",
                icon = Icons.Default.Mic,
                isGranted = hasAudioPermission,
                isMandatory = true,
                onGrantClick = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            PermissionCard(
                title = "Contacts Provider",
                subtitle = "Allows Mir to look up phone numbers so you can say 'Call Sarah' or 'Message Mom'.",
                icon = Icons.Default.Contacts,
                isGranted = hasContactsPermission,
                isMandatory = false,
                onGrantClick = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            PermissionCard(
                title = "Direct Calling",
                subtitle = "Enables Mir to place calls directly without stopping at the dialer keypad.",
                icon = Icons.Default.Call,
                isGranted = hasCallPermission,
                isMandatory = false,
                onGrantClick = {
                    permissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionCard(
                    title = "Background Notifications",
                    subtitle = "Keeps the persistent foreground service running so 'Mir' wakes up hands-free.",
                    icon = Icons.Default.Notifications,
                    isGranted = hasNotificationPermission,
                    isMandatory = false,
                    onGrantClick = {
                        permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // Bottom CTA Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (hasAudioPermission) {
                        onCompleteOnboarding()
                    } else {
                        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasAudioPermission) CyberCyan else ObsidianSurfaceContainer,
                    contentColor = if (hasAudioPermission) ObsidianDark else TextSecondary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("onboarding_continue_button")
            ) {
                Text(
                    text = if (hasAudioPermission) "Awaken Mir & Enter Void" else "Grant Microphone (Required)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    val perms = mutableListOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.CALL_PHONE
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        perms.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(perms.toTypedArray())
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("grant_all_permissions_button")
            ) {
                Text(
                    text = "Grant All Permissions At Once",
                    color = CyberViolet,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isGranted: Boolean,
    isMandatory: Boolean,
    onGrantClick: () -> Unit
) {
    val borderColor = if (isGranted) NeonGreen.copy(alpha = 0.45f) else ObsidianSurfaceContainer
    val iconTint = if (isGranted) NeonGreen else CyberCyan

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianSurfaceContainer)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(ObsidianDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.Check else icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (isMandatory) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "REQUIRED",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = TextTertiary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (isGranted) {
            Text(
                text = "READY",
                color = NeonGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            OutlinedButton(
                onClick = onGrantClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("grant_btn_${title.take(4).lowercase()}")
            ) {
                Text(text = "Grant", color = CyberCyan, fontSize = 12.sp)
            }
        }
    }
}
