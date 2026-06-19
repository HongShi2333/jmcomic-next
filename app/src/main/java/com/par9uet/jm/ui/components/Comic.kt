package com.par9uet.jm.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.par9uet.jm.data.models.Comic
import com.par9uet.jm.ui.screens.LocalMainNavController
import com.par9uet.jm.ui.viewModel.ComicDetailViewModel
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun Comic(
    comic: Comic,
    modifier: Modifier = Modifier,
    comicDetailViewModel: ComicDetailViewModel = koinActivityViewModel()
) {
    val mainNavController = LocalMainNavController.current
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        onClick = {
            comicDetailViewModel.reset(comic.id)
            mainNavController.navigate("comicDetail/${comic.id}")
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ComicCoverImage(comic)
            Text(
                modifier = Modifier
                    .padding(horizontal = 8.dp),
                text = comic.name,
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                fontSize = 13.sp,
                lineHeight = 16.sp,
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp),
                text = comic.authorList.joinToString(",").ifBlank { "暂无作者" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
