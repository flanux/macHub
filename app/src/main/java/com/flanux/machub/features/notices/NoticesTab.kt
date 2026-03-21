package com.flanux.machub.features.notices

import android.content.Context
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flanux.machub.data.DataRepository
import com.flanux.machub.data.Notice
import com.flanux.machub.data.Attachment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ViewModel
class NoticeViewModel(private val repository: DataRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedBatch = MutableStateFlow<String?>(null)
    val selectedBatch: StateFlow<String?> = _selectedBatch
    
    private val _expandedNoticeId = MutableStateFlow<String?>(null)
    val expandedNoticeId: StateFlow<String?> = _expandedNoticeId

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val allNotices: List<Notice>,
            val filteredNotices: List<Notice>,
            val categories: List<String>,
            val batches: List<String>
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    init {
        loadNotices()
    }

    fun loadNotices() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = repository.loadNotices()
                val notices = response.notices
                
                val categories = notices
                    .map { it.category }
                    .distinct()
                    .sorted()
                
                val batches = notices
                    .flatMap { it.batches }
                    .distinct()
                    .sortedDescending()
                
                _uiState.value = UiState.Success(
                    allNotices = notices,
                    filteredNotices = notices,
                    categories = categories,
                    batches = batches
                )
                
                applyFilters()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load notices")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
        applyFilters()
    }

    fun setBatch(batch: String?) {
        _selectedBatch.value = batch
        applyFilters()
    }
    
    fun toggleNoticeExpansion(noticeId: String) {
        _expandedNoticeId.value = if (_expandedNoticeId.value == noticeId) null else noticeId
    }

    private fun applyFilters() {
        val state = _uiState.value
        if (state !is UiState.Success) return

        var filtered = state.allNotices

        // Apply category filter
        _selectedCategory.value?.let { category ->
            filtered = filtered.filter { it.category == category }
        }

        // Apply batch filter
        _selectedBatch.value?.let { batch ->
            filtered = filtered.filter { batch in it.batches }
        }

        // Apply search filter
        val query = _searchQuery.value
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.body.contains(query, ignoreCase = true)
            }
        }

        _uiState.value = state.copy(filteredNotices = filtered)
    }
}

// Main Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticesTab(viewModel: NoticeViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedBatch by viewModel.selectedBatch.collectAsState()
    val expandedNoticeId by viewModel.expandedNoticeId.collectAsState()
    
    val context = LocalContext.current

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
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(state.message)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadNotices() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                
                is NoticeViewModel.UiState.Success -> {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search notices...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, "Clear search")
                                }
                            }
                        },
                        singleLine = true
                    )
                    
                    // Category filter chips
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { viewModel.setCategory(null) },
                                label = { Text("All") }
                            )
                        }
                        items(state.categories) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { 
                                    viewModel.setCategory(
                                        if (selectedCategory == category) null else category
                                    )
                                },
                                label = { Text(category.capitalize()) }
                            )
                        }
                    }
                    
                    // Batch filter chips
                    if (state.batches.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedBatch == null,
                                    onClick = { viewModel.setBatch(null) },
                                    label = { Text("All Batches") }
                                )
                            }
                            items(state.batches) { batch ->
                                FilterChip(
                                    selected = selectedBatch == batch,
                                    onClick = { 
                                        viewModel.setBatch(
                                            if (selectedBatch == batch) null else batch
                                        )
                                    },
                                    label = { Text("Batch $batch") }
                                )
                            }
                        }
                    }
                    
                    Divider()
                    
                    // Notice count
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${state.filteredNotices.size} notices",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Notices list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.filteredNotices,
                            key = { it.id }
                        ) { notice ->
                            NoticeCard(
                                notice = notice,
                                isExpanded = expandedNoticeId == notice.id,
                                onToggleExpansion = { viewModel.toggleNoticeExpansion(notice.id) },
                                onOpenUrl = { url -> openUrl(context, url) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoticeCard(
    notice: Notice,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category badge & date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryBadge(notice.category)
                
                if (notice.dateStr.isNotBlank()) {
                    Text(
                        notice.dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Title
            Text(
                notice.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Batch & semester info
            if (notice.batches.isNotEmpty() || notice.semester.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (notice.batches.isNotEmpty()) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Batch ${notice.batches.joinToString(", ")}") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                    
                    if (notice.semester.isNotBlank()) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Sem ${notice.semester}") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
            
            // Body (when expanded)
            if (isExpanded && notice.body.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    notice.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Attachments
            if (notice.attachments.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    notice.attachments.forEach { attachment ->
                        AttachmentButton(
                            attachment = attachment,
                            onClick = { onOpenUrl(attachment.url) }
                        )
                    }
                }
            }
            
            // Expand/collapse button
            if (notice.body.isNotBlank() || notice.attachments.isEmpty()) {
                TextButton(
                    onClick = onToggleExpansion,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (isExpanded) "Show less" else "Show more")
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Open in browser button
            OutlinedButton(
                onClick = { onOpenUrl(notice.url) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("View on Website")
            }
        }
    }
}

@Composable
fun CategoryBadge(category: String) {
    val (color, icon) = when (category.lowercase()) {
        "examination" -> MaterialTheme.colorScheme.error to Icons.Default.Edit
        "iost" -> MaterialTheme.colorScheme.tertiary to Icons.Default.AccountBox
        "admission" -> MaterialTheme.colorScheme.secondary to Icons.Default.Person
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
                category.capitalize(),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AttachmentButton(
    attachment: Attachment,
    onClick: () -> Unit
) {
    val (icon, color) = when (attachment.type) {
        "pdf" -> Icons.Default.Edit to MaterialTheme.colorScheme.error
        "sharepoint" -> Icons.Default.Share to MaterialTheme.colorScheme.tertiary
        "gdrive" -> Icons.Default.Menu to MaterialTheme.colorScheme.secondary
        else -> Icons.Default.ArrowForward to MaterialTheme.colorScheme.primary
    }
    
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = color
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    attachment.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

private fun String.capitalize() = this.replaceFirstChar { 
    if (it.isLowerCase()) it.titlecase() else it.toString() 
}
