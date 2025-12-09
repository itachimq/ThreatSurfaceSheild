package com.vishnu.threatsurfacesheild.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA), // Blue for accents
    background = Color.Black,
    surface = Color(0xFF111827), // Card background
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color(0xFFF87171) // Red for errors
)

@Composable
fun ThreatSurfaceShieldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography, // Assuming a Typography.kt file exists
        content = content
    )
}
