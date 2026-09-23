package com.criscard90.ytmp3.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Palette ispirata a YouTube (dark mode). */
val YtRed = Color(0xFFFF0000)
val YtBackground = Color(0xFF0F0F0F)
val YtSurface = Color(0xFF212121)
val YtSurfaceVariant = Color(0xFF272727)
val YtTextSecondary = Color(0xFFAAAAAA)
val YtDivider = Color(0xFF3F3F3F)

private val YtColorScheme = darkColorScheme(
    primary = YtRed,
    onPrimary = Color.White,
    onPrimaryContainer = Color.White,
    secondary = YtRed,
    background = YtBackground,
    onBackground = Color.White,
    surface = YtSurface,
    onSurface = Color.White,
    surfaceVariant = YtSurfaceVariant,
    onSurfaceVariant = YtTextSecondary,
    outline = YtDivider,
    error = Color(0xFFFF6B6B),
)

/**
 * Tema sempre scuro con accento rosso, coerente con l'aspetto di YouTube.
 */
@Composable
fun YtTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YtColorScheme,
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp),
        ),
        content = content,
    )
}
