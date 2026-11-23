package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CineCraftLightColorScheme = lightColorScheme(
    primary = StudioPrimaryContainer,
    onPrimary = Color.White,
    primaryContainer = StudioPrimaryFixed,
    onPrimaryContainer = StudioOnPrimaryFixed,
    secondary = StudioSecondaryInteractive,
    onSecondary = Color.White,
    secondaryContainer = StudioSecondaryFixed,
    onSecondaryContainer = StudioOnSecondaryFixed,
    tertiary = StudioViolet,
    onTertiary = Color.White,
    tertiaryContainer = StudioTertiaryFixed,
    onTertiaryContainer = StudioOnTertiaryFixed,
    background = StudioAppCanvas,
    onBackground = StudioOnSurface,
    surface = StudioCardSurface,
    onSurface = StudioOnSurface,
    surfaceVariant = StudioSurfaceContainer,
    onSurfaceVariant = StudioOnSurfaceVariant,
    surfaceContainerLowest = StudioCardSurface,
    surfaceContainerLow = StudioSurfaceContainerLow,
    surfaceContainer = StudioSurfaceContainer,
    surfaceContainerHigh = StudioSurfaceContainerHigh,
    surfaceContainerHighest = StudioSurfaceContainerHighest,
    outline = StudioOutline,
    outlineVariant = StudioBorderSubtle,
    error = StudioError,
    onError = Color.White,
    errorContainer = StudioErrorContainer,
    onErrorContainer = StudioOnErrorContainer
)

val CineCraftShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun CineCraftTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CineCraftLightColorScheme,
        typography = CineCraftTypography,
        shapes = CineCraftShapes,
        content = content
    )
}
