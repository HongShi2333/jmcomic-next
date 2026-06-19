package com.par9uet.jm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.ui.screens.AppScreen
import com.par9uet.jm.ui.viewModel.GlobalViewModel
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun App(
    globalViewModel: GlobalViewModel = koinActivityViewModel(),
    toastManager: ToastManager = getKoin().get()
) {
    LaunchedEffect(Unit) {
        globalViewModel.init()
    }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        toastManager.message.collect { text ->
            snackbarHostState.showSnackbar(
                message = text,
                actionLabel = null,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        AppScreen()
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
                .imePadding()
        )
    }
}
