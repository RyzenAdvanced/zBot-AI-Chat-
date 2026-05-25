package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonJade,
    secondary = BrilliantCyan,
    tertiary = NebulaViolet,
    background = MidnightBg,
    surface = CarbonCard,
    onPrimary = Color(0xFF002D22),
    onSecondary = Color(0xFF00363D),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF1E2530),
    onSurfaceVariant = TextSecondary,
    outline = BorderColor
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = AccentAzure,
    tertiary = RoyalSapphire,
    background = LightPearlBg,
    surface = LightCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // We prioritize our beautiful brand colors for high-end vibe
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
