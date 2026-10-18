package com.par9uet.jm.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.par9uet.jm.utils.shimmer

// 根据 index 给出不同的宽度，模拟真实 Tab 的长短不一
private val widths = listOf(90.dp, 80.dp, 85.dp, 75.dp, 90.dp)

@Composable
fun TabSkeleton(index: Int) {
    val currentWidth = widths[index % widths.size]
    Box(
        modifier = Modifier
            .width(currentWidth) // 给一个基础宽度
            .clip(RoundedCornerShape(4.dp))
            .shimmer()
    ) {
        Text(text = "\u00A0")
    }
}
