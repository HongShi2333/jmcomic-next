package com.par9uet.jm.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.store.LocalSettingManager
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.ui.screens.LocalMainNavController
import com.par9uet.jm.ui.viewModel.ComicDetailViewModel
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Comic(
    comic: Comic,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onToggleSelected: (() -> Unit)? = null,
    comicDetailViewModel: ComicDetailViewModel = koinActivityViewModel(),
    localSettingManager: LocalSettingManager = getKoin().get(),
) {
    val mainNavController = LocalMainNavController.current
    val clipboardManager = LocalClipboardManager.current
    val toastManager: ToastManager = getKoin().get()
    val localSetting by localSettingManager.localSettingState.collectAsState()

    WatchComicCard(
        title = comic.name,
        subtitle = if (localSetting.showComicIdAsSubtitle) "JM${comic.id}" else comic.authorList.joinToString("、").ifBlank { "暂无作者" },
        modifier = modifier,
        editing = editing,
        selected = selected,
        onClick = {
            if (editing && onToggleSelected != null) onToggleSelected() else {
                comicDetailViewModel.reset(comic.id)
                mainNavController.navigate("comicDetail/${comic.id}")
            }
        },
        onLongClick = { onLongClick?.invoke() },
        cover = { ComicCoverImage(comic, modifier = Modifier.size(width = 36.dp, height = 48.dp)) },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WatchComicCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    cover: @Composable () -> Unit,
) {
    Card(modifier.fillMaxWidth().heightIn(min = 64.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = if (editing && selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cover()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (editing) Icon(if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (selected) "已选中" else "未选中", modifier = Modifier.size(20.dp))
        }
    }
}
