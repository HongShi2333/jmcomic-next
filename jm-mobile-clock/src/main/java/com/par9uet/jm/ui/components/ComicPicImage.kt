package com.par9uet.jm.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.ComicPicImageState
import com.par9uet.jm.data.models.ImageResultState
import kotlinx.coroutines.launch

@Composable
fun ComicPicImage(
    modifier: Modifier = Modifier,
    comicPicImageState: ComicPicImageState,
    contentScale: ContentScale = ContentScale.FillBounds
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val imageResult = comicPicImageState.imageResultState

    val retryImageDecode = {
        coroutineScope.launch {
            comicPicImageState.decode(context, forceReload = true)
        }
    }

    Box(modifier = modifier) {
        when (imageResult) {
            is ImageResultState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is ImageResultState.Failure -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(imageResult.reason, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    TextButton(
                        onClick = { retryImageDecode() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Text("重试")
                    }
                }
            }

            is ImageResultState.Success -> {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    bitmap = imageResult.decodeImageBitmap,
                    contentDescription = "第${comicPicImageState.index}张图片",
                )
            }

        }
    }
}
