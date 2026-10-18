package com.par9uet.jm.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.models.ComicPicImageState
import com.par9uet.jm.data.models.ImageResultState
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.utils.saveComicPageToGallery
import org.koin.compose.getKoin

@Composable
fun ComicPicImage(
    modifier: Modifier = Modifier,
    comicPicImageState: ComicPicImageState,
    contentScale: ContentScale = ContentScale.FillBounds
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val imageResult = comicPicImageState.imageResultState
    val toast: ToastManager = getKoin().get()
    val settings: LocalSettingManager = getKoin().get()
    val localSetting by settings.localSettingState.collectAsState()
    var saving by remember(comicPicImageState) { mutableStateOf(false) }
    var pendingImage by remember(comicPicImageState) { mutableStateOf<Bitmap?>(null) }
    var confirmationImage by remember(comicPicImageState) { mutableStateOf<Bitmap?>(null) }
    fun save(image: Bitmap) {
        if (saving) return
        saving = true
        coroutineScope.launch {
            try {
                saveComicPageToGallery(context, image, comicPicImageState.comicId, comicPicImageState.index + 1)
                toast.showAsync("已保存到相册")
            } catch (error: CancellationException) {
                throw error
            } catch (_: OutOfMemoryError) {
                toast.showAsync("内存不足，暂时无法保存图片")
            } catch (error: Exception) {
                toast.showAsync(error.message ?: "保存失败")
            } finally {
                saving = false
            }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val image = pendingImage
        pendingImage = null
        if (granted && image != null && settings.localSettingState.value.readerLongPressSaveEnabled) {
            save(image)
        } else if (!granted) toast.showAsync("保存图片需要存储权限")
    }

    LaunchedEffect(localSetting.readerLongPressSaveEnabled) {
        if (!localSetting.readerLongPressSaveEnabled) {
            confirmationImage = null
            pendingImage = null
        }
    }
    confirmationImage?.takeIf { localSetting.readerLongPressSaveEnabled }?.let { image ->
        AlertDialog(
            onDismissRequest = { confirmationImage = null },
            title = { Text("保存图片到相册？") },
            text = { Text("将保存第 ${comicPicImageState.index + 1} 张图片。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmationImage = null
                    if (settings.localSettingState.value.readerLongPressSaveEnabled) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        ) {
                            pendingImage = image
                            permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else save(image)
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { confirmationImage = null }) { Text("取消") } },
        )
    }
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
                    modifier = Modifier.align(Alignment.Center),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(imageResult.reason)
                    TextButton(
                        onClick = {
                            retryImageDecode()
                        }
                    ) {
                        Text("重试")
                    }
                }
            }

            is ImageResultState.Success -> {
                Image(
                    modifier = if (localSetting.readerLongPressSaveEnabled) {
                        Modifier.fillMaxSize().comicLongPress("保存此页到相册") {
                            if (!saving && pendingImage == null && confirmationImage == null) {
                                confirmationImage = imageResult.decodeImageBitmap.asAndroidBitmap()
                            }
                        }
                    } else Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    bitmap = imageResult.decodeImageBitmap,
                    contentDescription = "第${comicPicImageState.index + 1}张图片",
                )
            }

        }
    }
}
