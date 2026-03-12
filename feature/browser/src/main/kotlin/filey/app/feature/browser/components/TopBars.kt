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
    isDeepSearch: Boolean,
    onDeepSearchToggle: () -> Unit,
    onFilterClick: () -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(if (isDeepSearch) "Derin ara…" else "Klasörde ara…") },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Aramayı kapat")
            }
        },
        actions = {
            IconButton(onClick = onFilterClick) {
                Icon(Icons.Default.Tune, "Filtrele")
            }
            IconButton(onClick = onDeepSearchToggle) {
                Icon(
                    if (isDeepSearch) Icons.Default.FilterList else Icons.Default.FilterListOff,
                    contentDescription = if (isDeepSearch) "Klasör aramasına geç" else "Derin aramaya geç",
                    tint = if (isDeepSearch) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, "Temizle")
                }
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
    onCutSelected: () -> Unit,
    onRenameSelected: () -> Unit
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
            IconButton(onClick = onRenameSelected) {
                Icon(Icons.Default.Edit, "Toplu Yeniden Adlandır")
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
