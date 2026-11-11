package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.session.AssistantState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberPink
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SassyAmber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MirOrbCanvas(
    state: AssistantState,
    inputAmplitude: Float,
    outputAmplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbMotion")

    // Idle breathing animation
    val idleBreathing by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdleBreathing"
    )

    // Continuous rotation angle for thinking and idle rings
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == AssistantState.THINKING) 1200 else 8000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationAngle"
    )

    // Pulse animation for thinking state
    val thinkingPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ThinkingPulse"
    )

    // Smooth amplitude lerp for listening
    val smoothInputAmp = remember { Animatable(0f) }
    LaunchedEffect(inputAmplitude) {
        smoothInputAmp.animateTo(
            targetValue = inputAmplitude.coerceIn(0f, 1f),
            animationSpec = tween(70, easing = LinearEasing)
        )
    }

    // Smooth amplitude lerp for speaking
    val smoothOutputAmp = remember { Animatable(0f) }
    LaunchedEffect(outputAmplitude) {
        smoothOutputAmp.animateTo(
            targetValue = outputAmplitude.coerceIn(0f, 1f),
            animationSpec = tween(70, easing = LinearEasing)
        )
    }

    val primaryColor = when (state) {
        AssistantState.IDLE -> CyberCyan
        AssistantState.LISTENING -> NeonGreen
        AssistantState.THINKING -> CyberPink
        AssistantState.SPEAKING -> CyberViolet
        AssistantState.EXECUTING_TOOL -> SassyAmber
        AssistantState.ERROR -> Color(0xFFFF3366)
    }

    val secondaryColor = when (state) {
        AssistantState.IDLE -> CyberViolet
        AssistantState.LISTENING -> CyberCyan
        AssistantState.THINKING -> CyberViolet
        AssistantState.SPEAKING -> CyberCyan
        AssistantState.EXECUTING_TOOL -> NeonGreen
        AssistantState.ERROR -> SassyAmber
    }

    Box(
        modifier = modifier
            .size(280.dp)
            .testTag("mir_central_orb")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.28f

            when (state) {
                AssistantState.IDLE -> {
                    drawIdleOrb(
                        center = center,
                        radius = baseRadius * idleBreathing,
                        angle = rotationAngle,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }
                AssistantState.LISTENING -> {
                    drawListeningOrb(
                        center = center,
                        radius = baseRadius,
                        amplitude = smoothInputAmp.value,
                        angle = rotationAngle,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }
                AssistantState.THINKING, AssistantState.EXECUTING_TOOL -> {
                    drawThinkingOrb(
                        center = center,
                        radius = baseRadius * thinkingPulse,
                        angle = rotationAngle,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }
                AssistantState.SPEAKING -> {
                    drawSpeakingOrb(
                        center = center,
                        radius = baseRadius,
                        amplitude = smoothOutputAmp.value,
                        angle = rotationAngle,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }
                AssistantState.ERROR -> {
                    drawIdleOrb(
                        center = center,
                        radius = baseRadius,
                        angle = rotationAngle,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }
            }
        }

        // Center Icon Overlay
        val icon = when (state) {
            AssistantState.IDLE -> Icons.Default.Mic
            AssistantState.LISTENING -> Icons.Default.Mic
            AssistantState.THINKING -> Icons.Default.WbSunny
            AssistantState.SPEAKING -> Icons.AutoMirrored.Filled.VolumeUp
            AssistantState.EXECUTING_TOOL -> Icons.Default.WbSunny
            AssistantState.ERROR -> Icons.Default.MicOff
        }

        Icon(
            imageVector = icon,
            contentDescription = "Mir Presence Indicator",
            tint = Color.White.copy(alpha = 0.95f),
            modifier = Modifier.size(44.dp)
        )
    }
}

private fun DrawScope.drawIdleOrb(
    center: Offset,
    radius: Float,
    angle: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    // Outer atmospheric glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.25f),
                secondaryColor.copy(alpha = 0.08f),
                Color.Transparent
            ),
            center = center,
            radius = radius * 2.2f
        ),
        radius = radius * 2.2f,
        center = center
    )

    // Subtle outer ring
    drawCircle(
        color = primaryColor.copy(alpha = 0.35f),
        radius = radius * 1.35f,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Dashed / segment planetary ring
    val segments = 24
    for (i in 0 until segments) {
        val rad = Math.toRadians((angle + i * (360f / segments)).toDouble())
        val startDist = radius * 1.55f
        val endDist = startDist + (if (i % 2 == 0) 12f else 6f)
        val p1 = Offset(
            center.x + (startDist * cos(rad)).toFloat(),
            center.y + (startDist * sin(rad)).toFloat()
        )
        val p2 = Offset(
            center.x + (endDist * cos(rad)).toFloat(),
            center.y + (endDist * sin(rad)).toFloat()
        )
        drawLine(
            color = secondaryColor.copy(alpha = 0.4f),
            start = p1,
            end = p2,
            strokeWidth = 2.dp.toPx()
        )
    }

    // Core holographic sphere
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.9f),
                primaryColor.copy(alpha = 0.85f),
                secondaryColor.copy(alpha = 0.9f),
                Color(0xFF07080D)
            ),
            center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
            radius = radius * 1.1f
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawListeningOrb(
    center: Offset,
    radius: Float,
    amplitude: Float,
    angle: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    val dynamicRadius = radius * (1f + amplitude * 0.6f)

    // Pulsing aura responding to mic volume
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.45f * (0.3f + amplitude * 0.7f)),
                secondaryColor.copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = center,
            radius = dynamicRadius * 2.4f
        ),
        radius = dynamicRadius * 2.4f,
        center = center
    )

    // Radial waveform spikes matching mic input
    val spikeCount = 36
    for (i in 0 until spikeCount) {
        val rad = Math.toRadians((angle + i * (360f / spikeCount)).toDouble())
        // Modulate with pseudo-random phase and real-time amplitude
        val spikeLen = 10f + amplitude * 70f * ((sin(i * 1.8) + 1.2).toFloat())
        val startDist = dynamicRadius * 1.15f
        val endDist = startDist + spikeLen

        val p1 = Offset(
            center.x + (startDist * cos(rad)).toFloat(),
            center.y + (startDist * sin(rad)).toFloat()
        )
        val p2 = Offset(
            center.x + (endDist * cos(rad)).toFloat(),
            center.y + (endDist * sin(rad)).toFloat()
        )
        drawLine(
            color = if (i % 2 == 0) primaryColor else secondaryColor,
            start = p1,
            end = p2,
            strokeWidth = 2.5.dp.toPx()
        )
    }

    // Inner energetic core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                primaryColor,
                secondaryColor.copy(alpha = 0.8f),
                Color(0xFF07080D)
            ),
            center = center,
            radius = dynamicRadius
        ),
        radius = dynamicRadius,
        center = center
    )
}

private fun DrawScope.drawThinkingOrb(
    center: Offset,
    radius: Float,
    angle: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    // Outer spinning neon ring with gradient arc
    val ringRadius = radius * 1.45f
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                primaryColor,
                secondaryColor,
                Color.White,
                Color.Transparent,
                primaryColor
            ),
            center = center
        ),
        radius = ringRadius,
        center = center,
        style = Stroke(width = 4.dp.toPx())
    )

    // Counter-rotating particle bursts
    val particles = 12
    for (i in 0 until particles) {
        val rad = Math.toRadians((-angle * 1.5 + i * (360f / particles)).toDouble())
        val dist = ringRadius * 1.15f
        val px = (center.x + dist * cos(rad)).toFloat()
        val py = (center.y + dist * sin(rad)).toFloat()
        drawCircle(
            color = if (i % 2 == 0) primaryColor else Color.White,
            radius = 3.5.dp.toPx(),
            center = Offset(px, py)
        )
    }

    // Core pulsar
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                primaryColor,
                secondaryColor,
                Color(0xFF07080D)
            ),
            center = center,
            radius = radius
        ),
        radius = radius * 0.9f,
        center = center
    )
}

private fun DrawScope.drawSpeakingOrb(
    center: Offset,
    radius: Float,
    amplitude: Float,
    angle: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    val dynamicRadius = radius * (1f + amplitude * 0.45f)

    // Concentric rippling sound waves
    val waveCount = 3
    for (w in 1..waveCount) {
        val waveRadius = dynamicRadius * (1f + (w * 0.35f) * (0.5f + amplitude * 0.5f))
        val waveAlpha = (1f - (w / (waveCount + 1f))) * (0.3f + amplitude * 0.7f)
        drawCircle(
            color = primaryColor.copy(alpha = waveAlpha.coerceIn(0f, 1f)),
            radius = waveRadius,
            center = center,
            style = Stroke(width = (3.5f - w * 0.8f).dp.toPx())
        )
    }

    // Sinusoidal fluid wave around circumference
    val path = Path()
    val points = 72
    for (i in 0..points) {
        val angleRad = (i.toFloat() / points) * 2f * PI.toFloat()
        val waveOffset = (sin((angleRad * 6f) + (angle * 0.1f)) * amplitude * 24f).toFloat()
        val currentR = dynamicRadius + waveOffset
        val x = center.x + currentR * cos(angleRad)
        val y = center.y + currentR * sin(angleRad)

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                primaryColor,
                secondaryColor,
                Color(0xFF07080D)
            ),
            center = center,
            radius = dynamicRadius
        )
    )
}
