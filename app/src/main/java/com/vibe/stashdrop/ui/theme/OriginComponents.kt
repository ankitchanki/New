package com.vibe.stashdrop.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibe.stashdrop.core.physics.IosSpringSpecs

/** Visual-only pill switch; the row around it carries the toggleable semantics. */
@Composable fun OriginSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val offset by animateDpAsState(if (checked) 20.dp else 0.dp, IosSpringSpecs.follow(), label = "switch knob")
    val track by animateColorAsState(if (checked) MaterialTheme.colorScheme.primary else Origin.Track, label = "switch track")
    Box(modifier.size(50.dp, 30.dp).clip(CircleShape).background(track).padding(3.dp)) {
        Box(Modifier.offset(x = offset).size(24.dp).background(Color.White, CircleShape))
    }
}

/** Rounded filter chip: ink when selected, white otherwise. */
@Composable fun OriginChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(20.dp))
        .background(if (selected) Origin.Ink else Origin.Card)
        .selectable(selected, role = Role.Tab, onClick = onClick).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (selected) Color.White else Origin.Ink, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
    }
}

/** Upper-case caption above a group of settings. */
@Composable fun GroupLabel(text: String) {
    Text(text.uppercase(), Modifier.padding(start = 8.dp, top = 8.dp), color = Origin.Muted,
        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = .6.sp, maxLines = 1)
}

/** White rounded group; rows inside are separated by inset hairlines via [GroupDivider]. */
@Composable fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Origin.Card).padding(vertical = 4.dp), content = content)
}
@Composable fun GroupDivider() { Box(Modifier.padding(start = 66.dp).fillMaxWidth().height(1.dp).background(Origin.Hairline)) }

/** Coloured rounded-square icon used at the start of setting rows. */
@Composable fun IconBadge(icon: ImageVector, background: Color, tint: Color = Color.White, size: Dp = 36.dp, corner: Dp = 12.dp) {
    Box(Modifier.size(size).background(background, RoundedCornerShape(corner)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size * .5f), tint = tint)
    }
}
