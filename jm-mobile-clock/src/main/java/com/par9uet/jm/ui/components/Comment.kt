package com.par9uet.jm.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.par9uet.jm.data.models.Comment
import com.par9uet.jm.store.RemoteSettingManager
import org.koin.compose.getKoin

@Composable
fun Comment(
    comment: Comment,
    showSource: Boolean = false,
    remoteSettingManager: RemoteSettingManager = getKoin().get(),
    onClick: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    val remoteSetting by remoteSettingManager.remoteSettingState.collectAsState()
    val avatarModel = if (comment.avatar.startsWith("http://", ignoreCase = true) ||
        comment.avatar.startsWith("https://", ignoreCase = true)
    ) {
        comment.avatar
    } else {
        "${remoteSetting.imgHost}/media/users/${comment.avatar}"
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AsyncImage(
                model = avatarModel,
                contentDescription = "${comment.nickname}的头像",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
            )
            Text(comment.nickname, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            }
            Column {
                if (showSource) {
                    val sourceText = comment.sourceText()
                    if (sourceText.isNotBlank()) {
                        Text(
                            text = sourceText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                Text(
                    text = comment.time,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                val content = comment.content.toSafeAnnotatedComment()
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium
                )
                action?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    it.invoke()
                }
            }
        }
    }
}

private fun Comment.sourceText(): String {
    val comicText = when {
        sourceComicName.isNotBlank() -> sourceComicName
        comicId > 0 -> "漫画 #$comicId"
        else -> ""
    }
    val chapterText = sourceChapterId
        .takeIf { it.isNotBlank() && it != "0" }
        ?.let { "章节/位置 #$it" }
        .orEmpty()
    return listOf(comicText, chapterText)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
}

private fun String.toSafeAnnotatedComment(): AnnotatedString {
    if (isBlank()) return AnnotatedString("")
    return runCatching {
        AnnotatedString.fromHtml(htmlString = this)
    }.getOrElse {
        AnnotatedString(stripHtmlTags())
    }
}

private fun String.stripHtmlTags(): String {
    return replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</p\\s*>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]+>"), "")
        .trim()
}
