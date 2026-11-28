package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val IndustrialDarkScheme = darkColorScheme(
    primary = LabColors.AmberAccent,
    onPrimary = LabColors.VoidBg,
    primaryContainer = LabColors.AmberDim,
    onPrimaryContainer = LabColors.TextPrimary,
    secondary = LabColors.CyanAccent,
    onSecondary = LabColors.VoidBg,
    secondaryContainer = LabColors.CyanDim,
    onSecondaryContainer = LabColors.TextPrimary,
    background = LabColors.VoidBg,
    onBackground = LabColors.TextPrimary,
    surface = LabColors.PanelSurface,
    onSurface = LabColors.TextPrimary,
    surfaceVariant = LabColors.CardSurface,
    onSurfaceVariant = LabColors.TextSecondary,
    outline = LabColors.BorderSubtle,
    outlineVariant = LabColors.BorderStrong,
    error = LabColors.CrimsonWarn
)

private val LabShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp)
)

@Composable
fun KineticLabTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = IndustrialDarkScheme,
        typography = LabTypography,
        shapes = LabShapes,
        content = content
    )
}
