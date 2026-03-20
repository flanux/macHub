package com.flanux.machub.features.results

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flanux.machub.data.ResultRequest
import com.flanux.machub.data.StudentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ViewModel
class ResultViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Form)
    val uiState: StateFlow<UiState> = _uiState
    
    private val _symbolNumber = MutableStateFlow("")
    val symbolNumber: StateFlow<String> = _symbolNumber
    
    private val _dateOfBirth = MutableStateFlow("")
    val dateOfBirth: StateFlow<String> = _dateOfBirth
    
    private val _selectedSemester = MutableStateFlow(1)
    val selectedSemester: StateFlow<Int> = _selectedSemester
    
    sealed class UiState {
        object Form : UiState()
        object Loading : UiState()
        data class Success(val result: StudentResult) : UiState()
        data class Error(val message: String) : UiState()
        data class Cached(val result: StudentResult) : UiState()
    }
    
    fun setSymbolNumber(value: String) {
        _symbolNumber.value = value
    }
    
    fun setDateOfBirth(value: String) {
        _dateOfBirth.value = value
    }
    
    fun setSemester(value: Int) {
        _selectedSemester.value = value
    }
    
    fun checkResult() {
        if (_symbolNumber.value.isBlank()) {
            _uiState.value = UiState.Error("Please enter symbol number")
            return
        }
        
        if (_dateOfBirth.value.isBlank()) {
            _uiState.value = UiState.Error("Please enter date of birth")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                // TODO: Call result checker API/scraper
                // For now, show demo data
                kotlinx.coroutines.delay(2000)
                
                // Demo result
                val demoResult = StudentResult(
                    symbolNumber = _symbolNumber.value,
                    name = "Student Name",
                    semester = "${_selectedSemester.value}",
                    sgpa = "3.75",
                    cgpa = "3.68",
                    status = "Pass",
                    year = "2079",
                    subjects = listOf(
                        com.flanux.machub.data.SubjectResult(
                            code = "CSC251",
                            name = "Data Structure and Algorithm",
                            creditHour = "3",
                            gradePoint = "3.7",
                            grade = "A-",
                            marks = "85"
                        ),
                        com.flanux.machub.data.SubjectResult(
                            code = "CSC252",
                            name = "Operating System",
                            creditHour = "3",
                            gradePoint = "4.0",
                            grade = "A",
                            marks = "92"
                        )
                    )
                )
                
                _uiState.value = UiState.Success(demoResult)
                
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to fetch result")
            }
        }
    }
    
    fun resetForm() {
        _uiState.value = UiState.Form
        _symbolNumber.value = ""
        _dateOfBirth.value = ""
        _selectedSemester.value = 1
    }
}

// Main Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsTab(viewModel: ResultViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check Result") },
                actions = {
                    if (uiState is ResultViewModel.UiState.Success || uiState is ResultViewModel.UiState.Cached) {
                        IconButton(onClick = { viewModel.resetForm() }) {
                            Icon(Icons.Default.Refresh, "New Search")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is ResultViewModel.UiState.Form -> {
                    ResultForm(viewModel = viewModel)
                }
                is ResultViewModel.UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Fetching result from TU...")
                        }
                    }
                }
                is ResultViewModel.UiState.Success -> {
                    ResultDisplay(result = state.result)
                }
                is ResultViewModel.UiState.Cached -> {
                    ResultDisplay(result = state.result, isCached = true)
                }
                is ResultViewModel.UiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.resetForm() }) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultForm(viewModel: ResultViewModel) {
    val symbolNumber by viewModel.symbolNumber.collectAsState()
    val dateOfBirth by viewModel.dateOfBirth.collectAsState()
    val selectedSemester by viewModel.selectedSemester.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "TU Result Checker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Check your B.Sc. CSIT result instantly",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        OutlinedTextField(
            value = symbolNumber,
            onValueChange = { viewModel.setSymbolNumber(it) },
            label = { Text("Symbol Number") },
            placeholder = { Text("e.g., 7905123") },
            leadingIcon = { Icon(Icons.Default.Badge, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = dateOfBirth,
            onValueChange = { viewModel.setDateOfBirth(it) },
            label = { Text("Date of Birth") },
            placeholder = { Text("YYYY-MM-DD (e.g., 2002-05-15)") },
            leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Text(
            "Select Semester",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (1..4).forEach { sem ->
                FilterChip(
                    selected = selectedSemester == sem,
                    onClick = { viewModel.setSemester(sem) },
                    label = { Text("$sem") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (5..8).forEach { sem ->
                FilterChip(
                    selected = selectedSemester == sem,
                    onClick = { viewModel.setSemester(sem) },
                    label = { Text("$sem") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Button(
            onClick = { viewModel.checkResult() },
            modifier = Modifier.fillMaxWidth(),
            enabled = symbolNumber.isNotBlank() && dateOfBirth.isNotBlank()
        ) {
            Icon(Icons.Default.Search, null)
            Spacer(Modifier.width(8.dp))
            Text("Check Result")
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Result will be cached for offline viewing after first fetch",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun ResultDisplay(result: StudentResult, isCached: Boolean = false) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.status == "Pass") 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                result.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Symbol: ${result.symbolNumber}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Surface(
                            color = if (result.status == "Pass") 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                result.status.uppercase(),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Semester", style = MaterialTheme.typography.labelSmall)
                            Text(result.semester, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("SGPA", style = MaterialTheme.typography.labelSmall)
                            Text(result.sgpa, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        if (result.cgpa != null) {
                            Column {
                                Text("CGPA", style = MaterialTheme.typography.labelSmall)
                                Text(result.cgpa, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        Column {
                            Text("Year", style = MaterialTheme.typography.labelSmall)
                            Text(result.year, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        // Subjects Header
        item {
            Text(
                "Subject-wise Result",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Subject Cards
        items(result.subjects) { subject ->
            SubjectCard(subject = subject)
        }
        
        // Cached indicator
        if (isCached) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudOff, null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Viewing cached result (offline)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectCard(subject: com.flanux.machub.data.SubjectResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        subject.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${subject.code} • ${subject.creditHour} Credit Hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } 
                
                Surface(
                    color = when (subject.grade) {
                        "A" -> MaterialTheme.colorScheme.primaryContainer
                        "A-", "B+" -> MaterialTheme.colorScheme.secondaryContainer
                        "B", "B-", "C+" -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        subject.grade,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (subject.marks != null) {
                    Text("Marks: ${subject.marks}", style = MaterialTheme.typography.bodySmall)
                }
                Text("Grade Point: ${subject.gradePoint}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
