package com.omersusin.filey.feature.browser

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omersusin.filey.core.model.FileType
import com.omersusin.filey.core.ui.components.EmptyState
import com.omersusin.filey.core.ui.components.LoadingIndicator
import com.omersusin.filey.core.ui.components.PermissionScreen
import com.omersusin.filey.core.util.PermissionHelper
import com.omersusin.filey.feature.browser.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    initialPath: String = "/storage/emulated/0",
    onNavigate: (String) -> Unit = {},
    onOpenImage: (String) -> Unit = {},
    onOpenVideo: (String) -> Unit = {},
    onOpenAudio: (String) -> Unit = {},
    onOpenText: (String) -> Unit = {},
    onOpenArchive: (String) -> Unit = {},
    onBack: (() -> Unit)? = null,
    viewModel: BrowserViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var hasPermission by remember { mutableStateOf(PermissionHelper.hasStoragePermission(context)) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var selectedFileIndex by remember { mutableIntStateOf(-1) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasPermission = PermissionHelper.hasStoragePermission(context)
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.loadDirectory(initialPath)
    }

    if (!hasPermission) {
        PermissionScreen(onRequestPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                permissionLauncher.launch(intent)
            }
        })
        return
    }

    Scaffold(
        topBar = {
            if (state.isSearching) {
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onClose = { viewModel.toggleSearch() }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = state.currentPath.substringAfterLast("/").ifEmpty { "/" },
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.Search, "Ara")
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                if (state.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                "Görünüm"
                            )
                        }
                        IconButton(onClick = { showSortSheet = true }) {
                            Icon(Icons.Default.Sort, "Sırala")
                        }
                        IconButton(onClick = { showNewFolderDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, "Yeni Klasör")
                        }
                        if (state.clipboard != null) {
                            IconButton(onClick = { viewModel.paste() }) {
                                Icon(Icons.Default.ContentPaste, "Yapıştır")
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            PathBar(
                path = state.currentPath,
                onPathClick = { onNavigate(it) }
            )

            when {
                state.isLoading -> LoadingIndicator()
                state.files.isEmpty() -> EmptyState(message = "Bu klasör boş")
                state.isGridView -> FileGrid(
                    files = state.files,
                    onClick = { file ->
                        when {
                            file.isDirectory -> onNavigate(file.path)
                            file.type == FileType.IMAGE -> onOpenImage(file.path)
                            file.type == FileType.VIDEO -> onOpenVideo(file.path)
                            file.type == FileType.AUDIO -> onOpenAudio(file.path)
                            file.type == FileType.ARCHIVE -> onOpenArchive(file.path)
                            file.type == FileType.TEXT -> onOpenText(file.path)
                            else -> onOpenText(file.path)
                        }
                    },
                    onLongClick = { index ->
                        selectedFileIndex = index
                        showOptionsSheet = true
                    }
                )
                else -> FileList(
                    files = state.files,
                    onClick = { file ->
                        when {
                            file.isDirectory -> onNavigate(file.path)
                            file.type == FileType.IMAGE -> onOpenImage(file.path)
                            file.type == FileType.VIDEO -> onOpenVideo(file.path)
                            file.type == FileType.AUDIO -> onOpenAudio(file.path)
                            file.type == FileType.ARCHIVE -> onOpenArchive(file.path)
                            file.type == FileType.TEXT -> onOpenText(file.path)
                            else -> onOpenText(file.path)
                        }
                    },
                    onLongClick = { index ->
                        selectedFileIndex = index
                        showOptionsSheet = true
                    }
                )
            }
        }
    }

    if (showSortSheet) {
        SortSheet(
            currentSort = state.sortOption,
            showHidden = state.showHidden,
            onSortSelected = { viewModel.setSortOption(it) },
            onToggleHidden = { viewModel.toggleShowHidden() },
            onDismiss = { showSortSheet = false }
        )
    }

    if (showOptionsSheet && selectedFileIndex in state.files.indices) {
        val file = state.files[selectedFileIndex]
        FileOptionsSheet(
            file = file,
            onCopy = { viewModel.copyFile(file); showOptionsSheet = false },
            onCut = { viewModel.cutFile(file); showOptionsSheet = false },
            onRename = { showOptionsSheet = false; showRenameDialog = true },
            onDelete = { showOptionsSheet = false; showDeleteDialog = true },
            onDismiss = { showOptionsSheet = false }
        )
    }

    if (showNewFolderDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Yeni Klasör") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Klasör adı") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.createFolder(name)
                        showNewFolderDialog = false
                    }
                }) { Text("Oluştur") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("İptal") }
            }
        )
    }

    if (showRenameDialog && selectedFileIndex in state.files.indices) {
        val file = state.files[selectedFileIndex]
        var newName by remember { mutableStateOf(file.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Yeniden Adlandır") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Yeni ad") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        viewModel.renameFile(file, newName)
                        showRenameDialog = false
                    }
                }) { Text("Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("İptal") }
            }
        )
    }

    if (showDeleteDialog && selectedFileIndex in state.files.indices) {
        val file = state.files[selectedFileIndex]
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Sil") },
            text = { Text("${file.name} silinsin mi?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFile(file)
                    showDeleteDialog = false
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("İptal") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Dosya ara...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kapat")
            }
        }
    )
}
