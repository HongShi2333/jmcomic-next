package com.par9uet.jm.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.par9uet.jm.database.dao.DownloadComicDao
import com.par9uet.jm.database.model.DownloadComic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloadComicDetailViewModel(
    private val downloadComicDao: DownloadComicDao
) : ViewModel() {
    private val _comic = MutableStateFlow<DownloadComic?>(null)
    val comic = _comic.asStateFlow()

    fun load(id: Int) {
        viewModelScope.launch {
            _comic.value = downloadComicDao.getById(id)
        }
    }
}
