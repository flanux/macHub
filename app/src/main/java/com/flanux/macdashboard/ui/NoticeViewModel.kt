package com.flanux.macdashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flanux.macdashboard.data.Notice
import com.flanux.macdashboard.data.NoticeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NoticeUiState {
    object Loading : NoticeUiState()
    data class Success(val notices: List<Notice>) : NoticeUiState()
    data class Error(val message: String) : NoticeUiState()
}

class NoticeViewModel(
    private val repository: NoticeRepository = NoticeRepository()
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<NoticeUiState>(NoticeUiState.Loading)
    val uiState: StateFlow<NoticeUiState> = _uiState.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()
    
    private val _selectedBatch = MutableStateFlow<String?>(null)
    val selectedBatch: StateFlow<String?> = _selectedBatch.asStateFlow()
    
    init {
        loadNotices()
    }
    
    fun loadNotices() {
        viewModelScope.launch {
            _uiState.value = NoticeUiState.Loading
            
            val flow = when {
                _selectedBatch.value != null -> repository.getNoticesByBatch(_selectedBatch.value!!)
                _selectedCategory.value != "all" -> repository.getNoticesByCategory(_selectedCategory.value)
                else -> repository.getNotices()
            }
            
            flow.collect { result ->
                _uiState.value = result.fold(
                    onSuccess = { NoticeUiState.Success(it) },
                    onFailure = { NoticeUiState.Error(it.message ?: "Unknown error") }
                )
            }
        }
    }
    
    fun setCategory(category: String) {
        _selectedCategory.value = category
        loadNotices()
    }
    
    fun setBatch(batch: String?) {
        _selectedBatch.value = batch
        loadNotices()
    }
    
    fun refresh() {
        loadNotices()
    }
}
