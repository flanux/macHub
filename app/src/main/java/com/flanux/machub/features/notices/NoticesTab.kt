package com.flanux.machub.features.notices

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flanux.machub.data.ApiService
import com.flanux.machub.data.Notice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ViewModel
class NoticeViewModel : ViewModel() {
    private val api = ApiService.create()

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedBatch = MutableStateFlow<String?>(null)
    val selectedBatch: StateFlow<String?> = _selectedBatch

    sealed class UiState {
        object Loading : UiState()
        data class Success(val notices: List<Notice>) : UiState()
        data class Error(val message: String) : UiState()
    }

    init {
        loadNotices()
    }

    fun loadNotices() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = api.getNotices()
                _uiState.value = UiState.Success(response.notices)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setBatch(batch: String?) {
        _selectedBatch.value = batch
    }
}

// Main Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticesTab(viewModel: NoticeViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedBatch by viewModel.selectedBatch.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notices") },
                actions = {
                    IconButton(onClick = { viewModel.loadNotices() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is NoticeViewModel.UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is NoticeViewModel.UiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error: ${state.message}")
                            Button(onClick = { viewModel.loadNotices() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is NoticeViewModel.UiState.Success -> {
                    val filteredNotices = state.notices.filter { notice ->
                        (selectedCategory == null || notice.category == selectedCategory) &&
                        (selectedBatch == null || notice.batch == selectedBatch)
                    }

                    FilterSection(
                        categories = state.notices.map { it.category }.distinct(),
                        batches = state.notices.mapNotNull { it.batch }.distinct().sorted(),
                        selectedCategory = selectedCategory,
                        selectedBatch = selectedBatch,
                        onCategoryChange = { viewModel.setCategory(it) },
                        onBatchChange = { viewModel.setBatch(it) }
                    )

                    NoticeList(notices = filteredNotices)
                }
            }
        }
    }
}

@Composable
fun FilterSection(
    categories: List<String>,
    batches: List<String>,
    selectedCategory: String?,
    selectedBatch: String?,
    onCategoryChange: (String?) -> Unit,
    onBatchChange: (String?) -> Unit
) {
    Column(
        modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Category",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategoryChange(null) },
                    label = { Text("All") }
                )
            }
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(category.capitalize()) }
                )
            }
        }

        Text(
            "Batch",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedBatch == null,
                    onClick = { onBatchChange(null) },
                    label = { Text("All") }
                )
            }
            items(batches) { batch ->
                FilterChip(
                    selected = selectedBatch == batch,
                    onClick = { onBatchChange(batch) },
                    label = { Text(batch) }
                )
            }
        }
    }
}

@Composable
fun NoticeList(notices: List<Notice>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(notices) { notice ->
            NoticeCard(notice = notice)
        }
    }
}

@Composable
fun NoticeCard(notice: Notice) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
        .fillMaxWidth()
        .clickable {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(notice.url))
            context.startActivity(intent)
        },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryBadge(category = notice.category)

                if (notice.batch != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = notice.batch,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Text(
                text = notice.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap to open",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryBadge(category: String) {
    val (bgColor, textColor) = when (category) {
        "result" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        "exam" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = category.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

fun String.capitalize() = replaceFirstChar { it.uppercase() }
