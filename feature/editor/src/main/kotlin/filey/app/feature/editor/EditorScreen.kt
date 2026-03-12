package filey.app.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    path: String,
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel(factory = EditorViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(path) {
        viewModel.loadFile(path)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("Kaydedildi ✓")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.fileName, maxLines = 1)
                        if (uiState.hasChanges) {
                            Text(
                                "• Değişiklik var",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSearch() }) {
                        Icon(Icons.Default.Search, "Ara")
                    }

                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 12.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = { viewModel.save() },
                            enabled = uiState.hasChanges
                        ) {
                            Icon(
                                Icons.Default.Save, "Kaydet",
                                tint = if (uiState.hasChanges)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (uiState.isSearchOpen) {
                SearchReplaceBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    replace = uiState.replaceQuery,
                    onReplaceChange = { viewModel.updateReplaceQuery(it) },
                    results = uiState.searchResults,
                    currentIndex = uiState.currentMatchIndex,
                    onNext = { viewModel.nextMatch() },
                    onPrev = { viewModel.previousMatch() },
                    onReplaceCurrent = { viewModel.replaceCurrent() },
                    onReplaceAll = { viewModel.replaceAll() }
                )
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.content.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "Bilinmeyen hata",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    val scrollState = rememberScrollState()
                    val lineCount = uiState.content.lines().size
                    
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Line numbers
                        Column(
                            modifier = Modifier
                                .width(44.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .verticalScroll(scrollState)
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            for (i in 1..lineCount) {
                                Text(
                                    text = "$i",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }

                        // Editor content
                        BasicTextField(
                            value = uiState.content,
                            onValueChange = { viewModel.updateContent(it) },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchReplaceBar(
    query: String,
    onQueryChange: (String) -> Unit,
    replace: String,
    onReplaceChange: (String) -> Unit,
    results: List<Int>,
    currentIndex: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onReplaceCurrent: () -> Unit,
    onReplaceAll: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Bul…") },
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        if (results.isNotEmpty()) {
                            Text(
                                "${currentIndex + 1}/${results.size}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp)
                )
                IconButton(onClick = onPrev, enabled = results.isNotEmpty()) {
                    Icon(Icons.Default.KeyboardArrowUp, null)
                }
                IconButton(onClick = onNext, enabled = results.isNotEmpty()) {
                    Icon(Icons.Default.KeyboardArrowDown, null)
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = replace,
                    onValueChange = onReplaceChange,
                    placeholder = { Text("Değiştir…") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp)
                )
                TextButton(onClick = onReplaceCurrent, enabled = currentIndex != -1) {
                    Text("Değiştir")
                }
                TextButton(onClick = onReplaceAll, enabled = query.isNotEmpty()) {
                    Text("Tümünü")
                }
            }
        }
    }
}
