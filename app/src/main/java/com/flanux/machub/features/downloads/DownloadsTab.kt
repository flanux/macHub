package com.flanux.machub.features.downloads

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flanux.machub.data.ApiService
import com.flanux.machub.data.Download
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ViewModel
class DownloadViewModel : ViewModel() {
    private val api = ApiService.create()
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState
    
    sealed class UiState {
        object Loading : UiState()
        data class Success(val downloads: List<Download>) : UiState()
        data class Error(val message: String) : UiState()
    }
    
    init {
        loadDownloads()
    }
    
    fun loadDownloads() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = api.getDownloads()
                _uiState.value = UiState.Success(response.downloads)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// Main Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsTab(viewModel: DownloadViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                actions = {
                    IconButton(onClick = { viewModel.loadDownloads() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is DownloadViewModel.UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is DownloadViewModel.UiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error: ${state.message}")
                            Button(onClick = { viewModel.loadDownloads() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is DownloadViewModel.UiState.Success -> {
                    val studentDownloads = state.downloads.filter { it.category == "student" }
                    val generalDownloads = state.downloads.filter { it.category == "general" }
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (studentDownloads.isNotEmpty()) {
                            item {
                                Text(
                                    "Student Downloads",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(studentDownloads) { download ->
                                DownloadCard(download = download)
                            }
                        }
                        
                        if (generalDownloads.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "General Downloads",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(generalDownloads) { download ->
                                DownloadCard(download = download)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadCard(download: Download) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(download.url))
                context.startActivity(intent)
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File type icon
            val icon = when (download.type.uppercase()) {
                "PDF" -> Icons.Default.PictureAsPdf
                "DOC", "DOCX" -> Icons.Default.Description
                "XLS", "XLSX" -> Icons.Default.TableChart
                else -> Icons.Default.InsertDriveFile
            }
            
            val iconColor = when (download.type.uppercase()) {
                "PDF" -> MaterialTheme.colorScheme.error
                "DOC", "DOCX" -> MaterialTheme.colorScheme.primary
                "XLS", "XLSX" -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            
            Icon(
                icon,
                contentDescription = download.type,
                modifier = Modifier.size(40.dp),
                tint = iconColor
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = download.type.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Default.Download,
                contentDescription = "Download",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
