package com.vibe.stashdrop.features.floatingdock

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import com.vibe.stashdrop.core.physics.IosSpringSpecs

/** Chat-head style drop target shown while the bubble is dragged; dropping the bubble on it hides the bubble. */
@Composable
fun DismissTarget(shown: Boolean, hot: Boolean, modifier: Modifier = Modifier) {
    val visible by animateFloatAsState(if (shown) 1f else 0f, IosSpringSpecs.follow(), label = "dismiss visible")
    val grow by animateFloatAsState(if (hot) 1.25f else 1f, IosSpringSpecs.landing(), label = "dismiss grow")
    Box(modifier.fillMaxSize().graphicsLayer { alpha = visible }
        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .55f)))), contentAlignment = Alignment.Center) {
        Box(Modifier.size(60.dp).graphicsLayer { scaleX = grow * (.6f + .4f * visible); scaleY = scaleX; translationY = (1 - visible) * 60 * density }
            .background(if (hot) Color(0xFFE5484D) else Color(0xFF242A39), CircleShape)
            .border(1.5.dp, Color.White.copy(alpha = if (hot) .9f else .5f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Close, "Drop here to hide the bubble", Modifier.size(28.dp), tint = Color.White)
        }
    }
}
