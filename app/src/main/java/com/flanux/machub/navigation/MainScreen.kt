package com.flanux.machub.navigation

import android.app.Application
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flanux.machub.data.DataRepository
import com.flanux.machub.features.downloads.DownloadsTab
import com.flanux.machub.features.downloads.DownloadsViewModel
import com.flanux.machub.features.gallery.GalleryTab
import com.flanux.machub.features.gallery.GalleryViewModel
import com.flanux.machub.features.info.InfoTab
import com.flanux.machub.features.news.NewsTab
import com.flanux.machub.features.news.NewsViewModel
import com.flanux.machub.features.notices.NoticesTab
import com.flanux.machub.features.notices.NoticeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val repository = remember { DataRepository(context) }
    
    var selectedTab by remember { mutableStateOf(0) }
    
    val tabs = listOf(
        NavigationItem("Notices", Icons.Default.Notifications),
        NavigationItem("Downloads", Icons.Default.Build),
        NavigationItem("News", Icons.Default.Star),
        NavigationItem("Gallery", Icons.Default.Face),
        NavigationItem("Info", Icons.Default.Info)
    )
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> {
                val viewModel: NoticeViewModel = viewModel(
                    factory = NoticeViewModelFactory(repository)
                )
                NoticesTab(viewModel)
            }
            1 -> {
                val viewModel: DownloadsViewModel = viewModel(
                    factory = DownloadsViewModelFactory(repository)
                )
                DownloadsTab(viewModel)
            }
            2 -> {
                val viewModel: NewsViewModel = viewModel(
                    factory = NewsViewModelFactory(repository)
                )
                NewsTab(viewModel)
            }
            3 -> {
                val viewModel: GalleryViewModel = viewModel(
                    factory = GalleryViewModelFactory(repository)
                )
                GalleryTab(viewModel)
            }
            4 -> InfoTab()
        }
    }
}

data class NavigationItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

// ViewModel Factories
class NoticeViewModelFactory(
    private val repository: DataRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return com.flanux.machub.features.notices.NoticeViewModel(repository) as T
    }
}

class DownloadsViewModelFactory(
    private val repository: DataRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return com.flanux.machub.features.downloads.DownloadsViewModel(repository) as T
    }
}

class NewsViewModelFactory(
    private val repository: DataRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return com.flanux.machub.features.news.NewsViewModel(repository) as T
    }
}

class GalleryViewModelFactory(
    private val repository: DataRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return com.flanux.machub.features.gallery.GalleryViewModel(repository) as T
    }
}
