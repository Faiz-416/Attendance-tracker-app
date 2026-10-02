package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = CharcoalBlack,
    primaryContainer = DarkEmerald,
    onPrimaryContainer = BrightMint,
    secondary = MintSecondary,
    onSecondary = CharcoalBlack,
    secondaryContainer = SubtleGreenSurface,
    onSecondaryContainer = BrightMint,
    tertiary = BrightMint,
    onTertiary = CharcoalBlack,
    background = CharcoalBlack,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = BorderDark,
    outlineVariant = CardBorder,
    error = AbsentRed,
    onError = Color.White
)

@Composable
fun AttendanceTrackerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
