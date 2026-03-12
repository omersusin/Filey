package filey.app.feature.browser.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import filey.app.feature.browser.BrowserUiState
import filey.app.feature.browser.BrowserViewModel
import filey.app.core.model.AccessMode
import filey.app.core.model.ViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserTopBar(
    uiState: BrowserUiState,
    viewModel: BrowserViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onSettings: () -> Unit
) {
    when {
        uiState.isMultiSelectActive -> {
            MultiSelectTopBar(
                selectedCount = uiState.selectedFiles.size,
                onClose = { viewModel.clearSelection() },
                onSelectAll = { viewModel.selectAll() },
                onDeleteSelected = { /* Show delete dialog */ },
                onRenameSelected = { /* Show rename dialog */ },
                onCopySelected = {
                    viewModel.setClipboard(uiState.selectedFiles.toList(), isCut = false)
                    viewModel.clearSelection()
                    viewModel.showSnackbar("Kopyalandı")
                },
                onCutSelected = {
                    viewModel.setClipboard(uiState.selectedFiles.toList(), isCut = true)
                    viewModel.clearSelection()
                    viewModel.showSnackbar("Kesildi")
                }
            )
        }
        uiState.isSearchActive -> {
            SearchTopBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                isDeepSearch = uiState.isDeepSearch,
                onDeepSearchToggle = { viewModel.toggleDeepSearch() },
                onFilterClick = { /* Show filter sheet */ },
                onClose = { viewModel.toggleSearch() }
            )
        }
        else -> {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.pathSegments.lastOrNull()?.name ?: "Filey",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (uiState.canGoBack) {
                        IconButton(onClick = { viewModel.goBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                        }
                    } else if (uiState.currentPath != BrowserUiState.DEFAULT_PATH) {
                        IconButton(onClick = onNavigateToDashboard) {
                            Icon(Icons.Default.Home, "Ana Sayfa")
                        }
                    } else {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, "Menü")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSearch() }) {
                        Icon(Icons.Default.Search, "Ara")
                    }

                    IconButton(onClick = {
                        val newMode = if (uiState.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                        viewModel.setViewMode(newMode)
                    }) {
                        Icon(
                            if (uiState.viewMode == ViewMode.LIST) Icons.Default.GridView
                            else Icons.AutoMirrored.Filled.ViewList,
                            "Görünüm"
                        )
                    }

                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, "Daha fazla")
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Yenile") },
                            leadingIcon = { Icon(Icons.Default.Refresh, null) },
                            onClick = {
                                viewModel.refreshCurrentDirectory()
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(if (uiState.showHiddenFiles) "Gizlileri Gizle" else "Gizlileri Göster") 
                            },
                            leadingIcon = { 
                                Icon(if (uiState.showHiddenFiles) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) 
                            },
                            onClick = {
                                viewModel.toggleHiddenFiles()
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sırala") },
                            leadingIcon = { Icon(Icons.Default.Sort, null) },
                            onClick = { /* Show sort sheet */ expanded = false }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Ayarlar") },
                            leadingIcon = { Icon(Icons.Default.Settings, null) },
                            onClick = {
                                expanded = false
                                onSettings()
                            }
                        )
                    }
                }
            )
        }
    }
}
