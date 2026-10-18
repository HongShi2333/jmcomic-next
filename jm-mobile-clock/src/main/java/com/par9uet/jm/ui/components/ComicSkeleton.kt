package com.par9uet.jm.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.par9uet.jm.utils.shimmer

@Composable
fun ComicSkeleton(
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(36.dp, 48.dp).clip(RoundedCornerShape(6.dp)).shimmer())
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().height(16.dp).shimmer())
                Box(Modifier.fillMaxWidth(.65f).height(12.dp).shimmer())
            }
        }
    }
}
