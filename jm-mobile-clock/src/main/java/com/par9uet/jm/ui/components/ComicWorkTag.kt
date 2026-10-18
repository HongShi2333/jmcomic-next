package com.par9uet.jm.ui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.par9uet.jm.ui.screens.LocalMainNavController
import com.par9uet.jm.ui.theme.ExtendedTheme

@Composable
fun ComicWorkTag(label: String) {
    val mainNavController = LocalMainNavController.current
    AssistChip(
        border = null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = ExtendedTheme.colors.workTag.colorContainer
        ),
        onClick = {
            mainNavController.navigate("comicSearchResult/$label")
        },
        label = {
            Text(
                text = label,
            )
        })
}
