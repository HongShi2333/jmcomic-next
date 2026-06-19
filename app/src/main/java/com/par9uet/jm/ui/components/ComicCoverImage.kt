package com.par9uet.jm.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.store.RemoteSettingManager
import org.koin.compose.getKoin

@Composable
fun ComicCoverImage(
    comic: Comic,
    showIdChip: Boolean = false,
    remoteSettingManager: RemoteSettingManager = getKoin().get(),
    imageLoader: ImageLoader = getKoin().get()
) {
    val remoteSetting by remoteSettingManager.remoteSettingState.collectAsState()
    Box(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = "${remoteSetting.imgHost}/media/albums/${comic.id}_3x4.jpg",
            imageLoader = imageLoader,
            contentDescription = "${comic.name}的封面",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .aspectRatio(3f / 4f)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium),
        )
        if (showIdChip) {
            AssistChip(
                border = null,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = 10.dp),
                onClick = {},
                label = {
                    Text("JM${comic.id}")
                }
            )
        }
    }
}
