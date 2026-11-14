package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ViceMetroColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = MidnightAsphalt,
    primaryContainer = ElevatedSteel,
    onPrimaryContainer = ElectricCyan,
    secondary = NeonAmber,
    onSecondary = MidnightAsphalt,
    secondaryContainer = ElevatedSteel,
    onSecondaryContainer = NeonAmber,
    tertiary = SunsetCoral,
    onTertiary = TextPrimary,
    background = MidnightAsphalt,
    onBackground = TextPrimary,
    surface = ConcreteSlate,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedSteel,
    onSurfaceVariant = TextSecondary,
    outline = BorderSlate,
    error = WantedCrimson,
    onError = TextPrimary
)

@Composable
fun ViceMetroTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ViceMetroColorScheme,
        typography = ViceMetroTypography,
        content = content
    )
}
