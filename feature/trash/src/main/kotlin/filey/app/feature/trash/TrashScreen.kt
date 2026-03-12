package filey.app.feature.trash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import filey.app.core.model.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = viewModel(factory = TrashViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEmptyConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Çöp Kutusu") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                actions = {
                    if (uiState.files.isNotEmpty()) {
                        IconButton(onClick = { showEmptyConfirm = true }) {
                            Icon(Icons.Default.DeleteForever, "Çöpü Boşalt")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.files.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Çöp kutusu boş")
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.padding(padding)) {
                    items(uiState.files) { file ->
                        ListItem(
                            headlineContent = { Text(file.name) },
                            supportingContent = { 
                                Text("Silinme: ${FileUtils.formatDate(file.lastModified)}") 
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { viewModel.restore(file.path) }) {
                                        Icon(Icons.Default.Restore, "Geri Yükle", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { viewModel.deletePermanently(file.path) }) {
                                        Icon(Icons.Default.DeleteForever, "Kalıcı Sil", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { Text("Çöpü Boşalt?") },
            text = { Text("Tüm dosyalar kalıcı olarak silinecek. Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.emptyTrash()
                    showEmptyConfirm = false
                }) {
                    Text("Boşalt", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) {
                    Text("İptal")
                }
            }
        )
    }
}
