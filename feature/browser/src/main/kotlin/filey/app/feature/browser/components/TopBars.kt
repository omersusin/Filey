package filey.app.feature.browser.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Dosya ara…") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Aramayı kapat")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectTopBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onCopySelected: () -> Unit,
    onCutSelected: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount seçili") },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Seçimi kapat")
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, "Tümünü seç")
            }
            IconButton(onClick = onCopySelected) {
                Icon(Icons.Default.ContentCopy, "Kopyala")
            }
            IconButton(onClick = onCutSelected) {
                Icon(Icons.Default.ContentCut, "Kes")
            }
            IconButton(onClick = onDeleteSelected) {
                Icon(Icons.Default.Delete, "Sil")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}
