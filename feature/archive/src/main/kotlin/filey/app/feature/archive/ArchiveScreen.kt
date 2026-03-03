package filey.app.feature.archive

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import filey.app.core.model.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    path: String,
    onBack: () -> Unit,
    viewModel: ArchiveViewModel = viewModel(factory = ArchiveViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(path) { viewModel.loadArchive(path) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.extractSuccess) {
        if (uiState.extractSuccess) {
            snackbarHostState.showSnackbar("Çıkarma tamamlandı ✓")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.fileName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Extract to same directory as the archive
                            val parentDir = path.substringBeforeLast('/')
                            val archiveName = path.substringAfterLast('/')
                                .substringBeforeLast('.')
                            viewModel.extractTo("$parentDir/$archiveName")
                        },
                        enabled = !uiState.isExtracting && uiState.entries.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Unarchive, "Çıkar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Extract progress
            if (uiState.isExtracting) {
                LinearProgressIndicator(
                    progress = { uiState.extractProgress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Çıkarılıyor… %${(uiState.extractProgress * 100).toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.entries.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Arşiv boş veya okunamıyor")
                    }
                }
                else -> {
                    Text(
                        "${uiState.entries.size} öğe",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyColumn {
                        items(uiState.entries) { entry ->
                            ListItem(
                                leadingContent = {
                                    Icon(
                                        if (entry.isDirectory) Icons.Outlined.Folder
                                        else Icons.Outlined.InsertDriveFile,
                                        contentDescription = null
                                    )
                                },
                                headlineContent = { Text(entry.name, maxLines = 1) },
                                supportingContent = {
                                    if (!entry.isDirectory) {
                                        Text(
                                            "${FileUtils.formatSize(entry.size)} → ${FileUtils.formatSize(entry.compressedSize)}"
                                        )
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
