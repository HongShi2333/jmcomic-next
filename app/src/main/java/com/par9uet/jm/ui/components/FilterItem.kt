package com.par9uet.jm.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FilterItem(
    label: String,
    active: Boolean,
    onClick: (() -> Unit) = {}
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        contentColor = if (active) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            text = label,
            fontSize = 14.sp
        )
    }
}
