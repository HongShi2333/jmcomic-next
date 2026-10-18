package com.par9uet.jm.ui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ComicSearchHistoryTag(label: String, onClick: () -> Unit = {}) {
    AssistChip(
        border = null,
        onClick = onClick,
        label = {
            Text(label)
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}
