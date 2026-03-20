package com.flanux.machub.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flanux.machub.features.notices.NoticesTab
import com.flanux.machub.features.notices.NoticeViewModel
import com.flanux.machub.features.downloads.DownloadsTab
import com.flanux.machub.features.downloads.DownloadViewModel
import com.flanux.machub.features.gallery.GalleryTab
import com.flanux.machub.features.gallery.GalleryViewModel
import com.flanux.machub.features.info.InfoTab

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    
    // Create ViewModels once
    val noticeViewModel: NoticeViewModel = viewModel()
    val downloadViewModel: DownloadViewModel = viewModel()
    val galleryViewModel: GalleryViewModel = viewModel()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Notifications, "Notices") },
                    label = { Text("Notices") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Download, "Downloads") },
                    label = { Text("Downloads") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Photo, "Gallery") },
                    label = { Text("Gallery") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Info, "Info") },
                    label = { Text("Info") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> NoticesTab(viewModel = noticeViewModel)
                1 -> DownloadsTab(viewModel = downloadViewModel)
                2 -> GalleryTab(viewModel = galleryViewModel)
                3 -> InfoTab()
            }
        }
    }
}
