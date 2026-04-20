package com.kidpod.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Coral,
    onPrimary = SurfaceLight,
    primaryContainer = CoralLight,
    onPrimaryContainer = OnSurfaceLight,
    secondary = Teal,
    onSecondary = SurfaceLight,
    secondaryContainer = Teal.copy(alpha = 0.2f),
    onSecondaryContainer = TealDark,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = BackgroundLight,
    error = androidx.compose.ui.graphics.Color(0xFFFF4444)
)

private val DarkColorScheme = darkColorScheme(
    primary = CoralLight,
    onPrimary = BackgroundDark,
    primaryContainer = CoralDark,
    onPrimaryContainer = SurfaceLight,
    secondary = Teal,
    onSecondary = BackgroundDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark
)

@Composable
fun KidPodTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KidPodTypography,
        content = content
    )
}
