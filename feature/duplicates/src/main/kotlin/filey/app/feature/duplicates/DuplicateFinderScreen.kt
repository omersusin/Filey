package filey.app.feature.duplicates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import filey.app.core.model.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateFinderScreen(
    onBack: () -> Unit,
    viewModel: DuplicateFinderViewModel = viewModel(factory = DuplicateFinderViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yinelenen Dosyalar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    if (!uiState.isLoading) {
                        IconButton(onClick = { viewModel.scan() }) {
                            Icon(Icons.Default.Search, "Tara")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(uiState.progress, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (uiState.groups.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Kopya dosya bulunamadı veya henüz tarama yapılmadı.")
                        Button(onClick = { viewModel.scan() }, modifier = Modifier.padding(16.dp)) {
                            Text("Taramayı Başlat")
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    uiState.groups.forEach { group ->
                        item {
                            GroupHeader(group)
                        }
                        items(group.files) { file ->
                            DuplicateFileItem(file, onDelete = { viewModel.deleteFile(file.path) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupHeader(group: DuplicateGroup) {
    val first = group.files.first()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = first.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = FileUtils.formatSize(first.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun DuplicateFileItem(file: filey.app.core.model.FileModel, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(file.path, style = MaterialTheme.typography.bodySmall, maxLines = 2) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
