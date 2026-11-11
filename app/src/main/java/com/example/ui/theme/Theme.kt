package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MirDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = ObsidianDark,
    primaryContainer = ObsidianSurfaceHighlight,
    onPrimaryContainer = CyberCyan,
    secondary = CyberViolet,
    onSecondary = ObsidianDark,
    secondaryContainer = ObsidianSurfaceContainer,
    onSecondaryContainer = CyberViolet,
    tertiary = NeonGreen,
    onTertiary = ObsidianDark,
    background = ObsidianDark,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceContainer,
    onSurfaceVariant = TextSecondary,
    outline = CyberCyanDim.copy(alpha = 0.35f),
    error = DangerRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Preserve bespoke futuristic identity
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = ObsidianDark.toArgb()
                window.navigationBarColor = ObsidianDark.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = MirDarkColorScheme,
        typography = Typography,
        content = content
    )
}
