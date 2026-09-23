package com.criscard90.ytmp3.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.criscard90.ytmp3.R
import com.criscard90.ytmp3.download.DownloadManager
import com.criscard90.ytmp3.ui.components.VideoSheet
import com.criscard90.ytmp3.ui.theme.YtTextSecondary
import com.criscard90.ytmp3.youtube.VideoSearchItem

/**
 * Contenitore dell'app: tab "Cerca" / "Download", barra di ricerca e bottom sheet.
 */
@Composable
fun AppRoot(searchViewModel: SearchViewModel = viewModel()) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<VideoSearchItem?>(null) }
    val state = searchViewModel.state

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* il download prosegue anche senza notifiche */ }

    fun startDownload(video: VideoSearchItem) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        DownloadManager.enqueue(context, video)
    }

    Scaffold(
        topBar = {
            if (tab == 0) {
                SearchTopBar(
                    query = query,
                    onQueryChange = { query = it },
                    onSubmit = { searchViewModel.search(query) },
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text("Cerca") },
                    colors = tabColors(),
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(painterResource(R.drawable.ic_download), contentDescription = null) },
                    label = { Text("Download") },
                    colors = tabColors(),
                )
            }
        },
    ) { padding ->
        when (tab) {
            0 -> SearchScreen(
                state = state,
                modifier = Modifier.padding(padding),
                onRetry = { searchViewModel.search(query) },
                onSelect = { selected = it },
            )
            else -> DownloadsScreen(modifier = Modifier.padding(padding))
        }
    }

    selected?.let { video ->
        VideoSheet(
            video = video,
            onDismiss = { selected = null },
            onDownload = { startDownload(video) },
        )
    }
}

@Composable
private fun tabColors(): NavigationBarItemColors = NavigationBarItemDefaults.colors(
    selectedIconColor = Color.White,
    selectedTextColor = Color.White,
    unselectedIconColor = YtTextSecondary,
    unselectedTextColor = YtTextSecondary,
    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
)
