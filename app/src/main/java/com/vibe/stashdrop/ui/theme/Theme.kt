package com.vibe.stashdrop.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.*
import com.vibe.stashdrop.R

/** OriginOS-inspired light palette: soft ground, white cards, near-black ink. */
object Origin {
    val Ground = Color(0xFFF2F3F7)
    val Card = Color.White
    val Ink = Color(0xFF111318)
    val Muted = Color(0xFF5B6070)
    val Hairline = Color(0xFFECEDF2)
    val Track = Color(0xFFD9DCE5)
    val Mint = Color(0xFF7FE3B5)
    val Success = Color(0xFF1E7A4E)
    val Orange = Color(0xFFC25410)
    val OrangeSoft = Color(0xFFFFF1E7)
    val Danger = Color(0xFFE5484D)
}

/** Accent choices; each keeps white text above 4.5:1 contrast. */
val AccentChoices = listOf(
    "Blue" to Color(0xFF3D5AFE),
    "Sky" to Color(0xFF0068D6),
    "Violet" to Color(0xFF6E3FF3),
    "Orange" to Color(0xFFC8461B),
)
fun accentColor(index: Int) = AccentChoices.getOrElse(index) { AccentChoices[0] }.second

/** Outfit (SIL Open Font License, bundled as a variable font) for display and titles. */
@OptIn(ExperimentalTextApi::class)
val Outfit = FontFamily(
    Font(R.font.outfit_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.outfit_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.outfit_variable, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

private val typography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontFamily = Outfit, fontWeight = FontWeight.Bold),
        headlineLarge = t.headlineLarge.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold),
        headlineMedium = t.headlineMedium.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold),
        headlineSmall = t.headlineSmall.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontFamily = Outfit, fontWeight = FontWeight.SemiBold),
    )
}

@Composable fun StashDropTheme(accent: Int = 0, content: @Composable () -> Unit) {
    val primary = accentColor(accent)
    val soft = lerp(Color.White, primary, .1f)
    val colors = lightColorScheme(
        primary = primary, onPrimary = Color.White, primaryContainer = soft, onPrimaryContainer = Origin.Ink,
        secondary = Origin.Ink, onSecondary = Color.White, secondaryContainer = Origin.Ground, onSecondaryContainer = Origin.Ink,
        background = Origin.Ground, onBackground = Origin.Ink, surface = Origin.Card, onSurface = Origin.Ink,
        surfaceVariant = Origin.Ground, onSurfaceVariant = Origin.Muted, outline = Origin.Track, outlineVariant = Origin.Hairline,
        surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White, surfaceContainer = Color.White,
        surfaceContainerHigh = Color.White, surfaceContainerHighest = Origin.Ground,
        error = Color(0xFFB3261E), errorContainer = Color(0xFFFFE4E4), onErrorContainer = Color(0xFF8A1C20),
        inverseSurface = Origin.Ink, inverseOnSurface = Color.White, inversePrimary = soft,
    )
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
