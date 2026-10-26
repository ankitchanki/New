package com.vibe.stashdrop.ui.shelf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A completed swipe requests confirmation; it never silently destroys cached files. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeDeleteItem(onDeleteRequest: () -> Unit, content: @Composable () -> Unit) {
    val state=rememberSwipeToDismissBoxState(confirmValueChange={ value ->
        if(value!=SwipeToDismissBoxValue.Settled) onDeleteRequest()
        false
    },positionalThreshold={ it*.45f })
    SwipeToDismissBox(state,modifier=Modifier.semantics {
        customActions=listOf(CustomAccessibilityAction("Delete saved item") { onDeleteRequest(); true })
    },backgroundContent={
        Row(Modifier.fillMaxSize().background(if(state.dismissDirection==SwipeToDismissBoxValue.Settled) Color.Transparent else MaterialTheme.colorScheme.errorContainer,RoundedCornerShape(26.dp)).padding(horizontal=20.dp),
            horizontalArrangement=if(state.dismissDirection==SwipeToDismissBoxValue.StartToEnd) Arrangement.Start else Arrangement.End,
            verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Outlined.DeleteOutline,"Delete",tint=MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(6.dp))
            Text("Delete",color=MaterialTheme.colorScheme.onErrorContainer,style=MaterialTheme.typography.labelMedium)
        }
    }) { content() }
}
