package com.example.ui

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Phone
import com.example.session.AssistantState
import com.example.ui.components.HelpDialog
import com.example.ui.components.MirOrbCanvas
import com.example.ui.components.MirSettingsDialog
import com.example.ui.components.PermissionsOnboardingView
import com.example.ui.components.QuickActionRow
import com.example.ui.components.StateStatusBadge
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberPink
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
fun MirMainScreen(
    viewModel: MirViewModel,
    modifier: Modifier = Modifier
) {
    val isOnboardingNeeded by viewModel.isOnboardingNeeded.collectAsState()
    val hasAudioPermission by viewModel.hasAudioPermission.collectAsState()
    val hasContactsPermission by viewModel.hasContactsPermission.collectAsState()
    val hasCallPermission by viewModel.hasCallPermission.collectAsState()
    val hasNotificationPermission by viewModel.hasNotificationPermission.collectAsState()

    val assistantState by viewModel.assistantState.collectAsState()
    val subtitleText by viewModel.subtitleText.collectAsState()
    val lastToolAction by viewModel.lastToolAction.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    val inputAmplitude by viewModel.inputAmplitude.collectAsState()
    val outputAmplitude by viewModel.outputAmplitude.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isHelpOpen by viewModel.isHelpOpen.collectAsState()

    if (isOnboardingNeeded) {
        PermissionsOnboardingView(
            hasAudioPermission = hasAudioPermission,
            hasContactsPermission = hasContactsPermission,
            hasCallPermission = hasCallPermission,
            hasNotificationPermission = hasNotificationPermission,
            onPermissionRequested = {
                viewModel.checkPermissions()
            },
            onCompleteOnboarding = {
                viewModel.completeOnboarding()
            }
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = ObsidianDark
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF131826),
                            Color(0xFF090B10),
                            ObsidianDark
                        ),
                        radius = 1400f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Bar
                TopMirHeader(
                    connectionStatus = connectionStatus,
                    isBgActive = viewModel.preferences.isBackgroundServiceRunning,
                    onToggleBg = {
                        viewModel.toggleBackgroundService(!viewModel.preferences.isBackgroundServiceRunning)
                    },
                    onOpenSettings = { viewModel.openSettings() },
                    onOpenHelp = { viewModel.openHelp() }
                )

                // Central Focus Area (Mir Orb & Sassy Status)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    MirOrbCanvas(
                        state = assistantState,
                        inputAmplitude = inputAmplitude,
                        outputAmplitude = outputAmplitude,
                        onClick = { viewModel.onOrbClicked() },
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    StateStatusBadge(
                        state = assistantState,
                        customSubtitle = subtitleText.takeIf { it.isNotBlank() }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Audio Level Meter Indicators
                    AudioLevelIndicatorRow(
                        state = assistantState,
                        inputAmplitude = inputAmplitude,
                        outputAmplitude = outputAmplitude
                    )
                }

                // Bottom Controls & Quick Action Intent Launchers
                QuickActionRow(
                    onTriggerTool = { toolName, args ->
                        viewModel.executeManualTool(toolName, args)
                    },
                    lastToolAction = lastToolAction,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Settings Dialog
            if (isSettingsOpen) {
                MirSettingsDialog(
                    preferences = viewModel.preferences,
                    onDismiss = { viewModel.closeSettings() },
                    onSaveAndRestartSession = {
                        viewModel.reloadPreferencesAndRestartSession()
                    }
                )
            }

            // Help Dialog (WhatsApp Support: 7699935408)
            if (isHelpOpen) {
                HelpDialog(
                    onDismiss = { viewModel.closeHelp() }
                )
            }
        }
    }
}

@Composable
private fun TopMirHeader(
    connectionStatus: String,
    isBgActive: Boolean,
    onToggleBg: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App identity
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(NeonGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "MIR LIVE",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp
                )
                Text(
                    text = connectionStatus,
                    color = if (connectionStatus.contains("Connected")) NeonGreen else TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Action Toggles in Top Right
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Background wake button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isBgActive) NeonGreen.copy(alpha = 0.15f) else ObsidianSurfaceContainer)
                    .border(
                        1.dp,
                        if (isBgActive) NeonGreen.copy(alpha = 0.5f) else ObsidianSurfaceContainer,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(onClick = onToggleBg)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("toggle_bg_header_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = if (isBgActive) NeonGreen else TextTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBgActive) "BG ON" else "BG OFF",
                        color = if (isBgActive) NeonGreen else TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Top Right Help Option with WhatsApp Icon (7699935408)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0D2818))
                    .border(1.dp, Color(0xFF25D366).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenHelp)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("top_right_help_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "WhatsApp Help: 7699935408",
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Help",
                        color = Color(0xFF25D366),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Settings button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ObsidianSurfaceContainer)
                    .testTag("open_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Preferences",
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AudioLevelIndicatorRow(
    state: AssistantState,
    inputAmplitude: Float,
    outputAmplitude: Float
) {
    val activeAmp = when (state) {
        AssistantState.LISTENING -> inputAmplitude
        AssistantState.SPEAKING -> outputAmplitude
        else -> 0f
    }

    val activeColor = when (state) {
        AssistantState.LISTENING -> NeonGreen
        AssistantState.SPEAKING -> CyberViolet
        AssistantState.THINKING -> CyberPink
        else -> CyberCyan.copy(alpha = 0.3f)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(24.dp)
    ) {
        val barCount = 18
        for (i in 0 until barCount) {
            // Distance from center
            val distFromCenter = kotlin.math.abs(i - barCount / 2).toFloat()
            val falloff = (1f - distFromCenter / (barCount / 2f)).coerceIn(0.2f, 1f)
            val barHeight = (4f + activeAmp * 20f * falloff).coerceIn(4f, 22f).dp

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(activeColor)
            )
        }
    }
}
