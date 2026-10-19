package com.vibe.stashdrop.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ice = Color(0xFFB4C9FF)
val Mint = Color(0xFF9DE3CF)
private val colors = darkColorScheme(primary=Ice, secondary=Mint, background=Color(0xFF0C0E15), surface=Color(0xFF171B26),
    surfaceVariant=Color(0xFF242A39), onBackground=Color(0xFFF1F3FA), onSurface=Color(0xFFF1F3FA))
@Composable fun StashDropTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme=colors,content=content) }
