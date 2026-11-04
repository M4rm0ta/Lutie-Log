package com.example.lutielog.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// iOS 浅色主题
private val LightColorScheme = lightColorScheme(
    primary = IOSSystemBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = IOSSystemBlue.copy(alpha = 0.12f),
    onPrimaryContainer = IOSSystemBlue,
    secondary = IOSSystemGray,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = IOSSystemGray5,
    onSecondaryContainer = IOSLabel,
    tertiary = IOSSystemGreen,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = IOSSystemGreen.copy(alpha = 0.12f),
    onTertiaryContainer = IOSSystemGreen,
    error = IOSSystemRed,
    onError = androidx.compose.ui.graphics.Color.White,
    errorContainer = IOSSystemRed.copy(alpha = 0.12f),
    onErrorContainer = IOSSystemRed,
    background = IOSBackground,
    onBackground = IOSLabel,
    surface = IOSSecondaryBackground,
    onSurface = IOSLabel,
    surfaceVariant = IOSSystemGray6,
    onSurfaceVariant = IOSSecondaryLabel,
    surfaceTint = IOSSystemBlue,
    inverseSurface = IOSLabel,
    inverseOnSurface = IOSBackground,
    inversePrimary = IOSSystemBlue,
    outline = IOSSeparator,
    outlineVariant = IOSSystemGray4,
    scrim = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f)
)

// iOS 深色主题
private val DarkColorScheme = darkColorScheme(
    primary = IOSSystemBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = IOSSystemBlue.copy(alpha = 0.25f),
    onPrimaryContainer = IOSSystemBlue,
    secondary = IOSSystemGray2,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = IOSSystemGray,
    onSecondaryContainer = IOSLabelDark,
    tertiary = IOSSystemGreen,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = IOSSystemGreen.copy(alpha = 0.25f),
    onTertiaryContainer = IOSSystemGreen,
    error = IOSSystemRed,
    onError = androidx.compose.ui.graphics.Color.White,
    errorContainer = IOSSystemRed.copy(alpha = 0.25f),
    onErrorContainer = IOSSystemRed,
    background = IOSBackgroundDark,
    onBackground = IOSLabelDark,
    surface = IOSSecondaryBackgroundDark,
    onSurface = IOSLabelDark,
    surfaceVariant = IOSSecondaryBackgroundDark,
    onSurfaceVariant = IOSSecondaryLabelDark,
    surfaceTint = IOSSystemBlue,
    inverseSurface = IOSLabelDark,
    inverseOnSurface = IOSBackgroundDark,
    inversePrimary = IOSSystemBlue,
    outline = IOSSeparatorDark,
    outlineVariant = IOSSystemGray,
    scrim = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
)

@Composable
fun LutieLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
