package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.session.AssistantState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberPink
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.ObsidianSurfaceContainer
import com.example.ui.theme.SassyAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StateStatusBadge(
    state: AssistantState,
    customSubtitle: String?,
    modifier: Modifier = Modifier
) {
    val stateColor = when (state) {
        AssistantState.IDLE -> CyberCyan
        AssistantState.LISTENING -> NeonGreen
        AssistantState.THINKING -> CyberPink
        AssistantState.SPEAKING -> CyberViolet
        AssistantState.EXECUTING_TOOL -> SassyAmber
        AssistantState.ERROR -> Color(0xFFFF3366)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Pill Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(ObsidianSurfaceContainer.copy(alpha = 0.85f))
                .border(1.dp, stateColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 7.dp)
                .testTag("mir_status_badge")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(stateColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.label.uppercase(),
                    color = stateColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sassy Quote / Subtitle Text
        AnimatedContent(
            targetState = customSubtitle?.takeIf { it.isNotBlank() } ?: state.quote,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "SassyQuoteTransition"
        ) { displayText ->
            Text(
                text = "“$displayText”",
                color = if (customSubtitle.isNullOrBlank()) TextSecondary else TextPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .testTag("mir_quote_text")
            )
        }
    }
}
