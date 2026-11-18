package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MandateXLightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = OnRoyalBlue,
    primaryContainer = RoyalBlueContainer,
    onPrimaryContainer = RoyalBlue,
    secondary = EmeraldGreen,
    onSecondary = OnEmerald,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldGreen,
    tertiary = GoldAmber,
    onTertiary = OnRoyalBlue,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = CardWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHigh = CardWhite,
    outline = BorderLight,
    outlineVariant = BorderLight,
    error = AlertRed,
    onError = OnAlertRed,
    errorContainer = AlertRedContainer,
    onErrorContainer = AlertRed
)

val MandateXShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MandateXTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MandateXLightColorScheme,
        typography = MandateXTypography,
        shapes = MandateXShapes,
        content = content
    )
}
