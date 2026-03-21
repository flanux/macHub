package com.flanux.machub.features.downloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flanux.machub.data.DataRepository
import com.flanux.machub.data.Download
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ViewModel
class DownloadsViewModel(private val repository: DataRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _selectedSemester = MutableStateFlow<String?>(null)
    val selectedSemester: StateFlow<String?> = _selectedSemester

    private val _selectedType = MutableStateFlow<String?>(null)
    val selectedType: StateFlow<String?> = _selectedType
    
    private val _selectedSection = MutableStateFlow<String>("student")
    val selectedSection: StateFlow<String> = _selectedSection

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val allDownloads: List<Download>,
            val filteredDownloads: List<Download>,
            val semesters: List<String>,
            val types: List<String>,
            val programs: List<String>
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    init {
        loadDownloads()
    }

    fun loadDownloads() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = repository.loadDownloads()
                val downloads = response.downloads
                
                val semesters = downloads
                    .map { it.semester }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sortedBy { it.toIntOrNull() ?: 99 }
                
                val types = downloads
                    .map { it.type }
                    .distinct()
                    .sorted()
                
                val programs = downloads
                    .map { it.program }
                    .distinct()
                    .sorted()
                
                _uiState.value = UiState.Success(
                    allDownloads = downloads,
                    filteredDownloads = downloads,
                    semesters = semesters,
                    types = types,
                    programs = programs
                )
                
                applyFilters()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load downloads")
            }
        }
    }

    fun setSection(section: String) {
        _selectedSection.value = section
        applyFilters()
    }

    fun setSemester(semester: String?) {
        _selectedSemester.value = semester
        applyFilters()
    }

    fun setType(type: String?) {
        _selectedType.value = type
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        if (state !is UiState.Success) return

        var filtered = state.allDownloads
        
        // Apply section filter
        filtered = filtered.filter { it.section == _selectedSection.value }

        // Apply semester filter
        _selectedSemester.value?.let { semester ->
            filtered = filtered.filter { it.semester == semester }
        }

        // Apply type filter
        _selectedType.value?.let { type ->
            filtered = filtered.filter { it.type == type }
        }

        _uiState.value = state.copy(filteredDownloads = filtered)
    }
}

// Main Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsTab(viewModel: DownloadsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedSemester by viewModel.selectedSemester.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val selectedSection by viewModel.selectedSection.collectAsState()
    val context = LocalContext.current

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
        Column(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is DownloadsViewModel.UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                
                is DownloadsViewModel.UiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(state.message)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadDownloads() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                
                is DownloadsViewModel.UiState.Success -> {
                    // Section toggle (Student vs General)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedSection == "student",
                            onClick = { viewModel.setSection("student") },
                            label = { Text("Student Resources") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedSection == "general",
                            onClick = { viewModel.setSection("general") },
                            label = { Text("General Resources") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Semester filter chips
                    if (state.semesters.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedSemester == null,
                                    onClick = { viewModel.setSemester(null) },
                                    label = { Text("All Semesters") }
                                )
                            }
                            items(state.semesters) { semester ->
                                FilterChip(
                                    selected = selectedSemester == semester,
                                    onClick = { 
                                        viewModel.setSemester(
                                            if (selectedSemester == semester) null else semester
                                        )
                                    },
                                    label = { Text("Semester $semester") }
                                )
                            }
                        }
                    }
                    
                    // Type filter chips
                    if (state.types.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedType == null,
                                    onClick = { viewModel.setType(null) },
                                    label = { Text("All Types") }
                                )
                            }
                            items(state.types) { type ->
                                FilterChip(
                                    selected = selectedType == type,
                                    onClick = { 
                                        viewModel.setType(
                                            if (selectedType == type) null else type
                                        )
                                    },
                                    label = { Text(type) }
                                )
                            }
                        }
                    }
                    
                    Divider()
                    
                    // Download count
                    Text(
                        "${state.filteredDownloads.size} resources",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    // Downloads list grouped by semester
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val grouped = state.filteredDownloads.groupBy { it.semester }
                        
                        grouped.entries.sortedBy { it.key.toIntOrNull() ?: 99 }.forEach { (semester, downloads) ->
                            item {
                                Text(
                                    "Semester ${semester.ifBlank { "N/A" }}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            
                            items(
                                items = downloads,
                                key = { "${it.program}-${it.level}-${it.type}-${it.url}" }
                            ) { download ->
                                DownloadCard(
                                    download = download,
                                    onOpenUrl = { url -> openUrl(context, url) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadCard(
    download: Download,
    onOpenUrl: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Type badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypeBadge(download.type, download.attType)
                
                if (download.program.isNotBlank()) {
                    Text(
                        download.program,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Title
            Text(
                download.type,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            if (download.level.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    download.level,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Open button
            Button(
                onClick = { onOpenUrl(download.url) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    getIconForType(download.attType),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Open ${getTypeLabel(download.attType)}")
            }
        }
    }
}

@Composable
fun TypeBadge(type: String, attType: String) {
    val (color, icon) = when (attType) {
        "gdrive" -> MaterialTheme.colorScheme.tertiary to Icons.Default.Menu
        "sharepoint" -> MaterialTheme.colorScheme.secondary to Icons.Default.Share
        "pdf" -> MaterialTheme.colorScheme.error to Icons.Default.Edit
        else -> MaterialTheme.colorScheme.primary to Icons.Default.Info
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                getTypeLabel(attType),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun getIconForType(attType: String) = when (attType) {
    "gdrive" -> Icons.Default.Menu
    "sharepoint" -> Icons.Default.Share
    "pdf" -> Icons.Default.Edit
    else -> Icons.Default.ArrowForward
}

fun getTypeLabel(attType: String) = when (attType) {
    "gdrive" -> "Google Drive"
    "sharepoint" -> "SharePoint"
    "pdf" -> "PDF"
    else -> "Link"
}

fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}
