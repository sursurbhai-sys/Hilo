package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = UnicGold,
    onPrimary = MidnightArena,
    primaryContainer = Color(0xFF3E2D00),
    onPrimaryContainer = Color(0xFFFFE08A),
    secondary = ArcaneCyan,
    onSecondary = MidnightArena,
    secondaryContainer = Color(0xFF003847),
    onSecondaryContainer = Color(0xFFB3F0FF),
    tertiary = RoyalViolet,
    onTertiary = CrispWhite,
    background = MidnightArena,
    onBackground = TextPrimaryDark,
    surface = DeepSlate,
    onSurface = TextPrimaryDark,
    surfaceVariant = ElevatedCardDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF33416B),
    error = DangerRed,
    onError = CrispWhite
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD98200),
    onPrimary = CrispWhite,
    primaryContainer = Color(0xFFFFEAC2),
    onPrimaryContainer = Color(0xFF3B2200),
    secondary = Color(0xFF007EA7),
    onSecondary = CrispWhite,
    secondaryContainer = Color(0xFFD0F2FF),
    onSecondaryContainer = Color(0xFF002736),
    tertiary = RoyalViolet,
    onTertiary = CrispWhite,
    background = WarmIvory,
    onBackground = TextPrimaryLight,
    surface = CrispWhite,
    onSurface = TextPrimaryLight,
    surfaceVariant = SoftParchment,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFD5CEBC),
    error = DangerRed,
    onError = CrispWhite
)

@Composable
fun LudoUnicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
