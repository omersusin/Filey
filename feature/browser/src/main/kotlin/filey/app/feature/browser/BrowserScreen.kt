package filey.app.feature.browser
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
package filey.app.feature.browser


import android.content.Intent
import android.content.Intent
import android.net.Uri
import android.net.Uri
import android.os.Build
import android.os.Build
import android.provider.Settings
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import filey.app.core.model.FileType
import filey.app.core.model.FileType
import filey.app.core.ui.components.EmptyState
import filey.app.core.ui.components.EmptyState
import filey.app.core.ui.components.LoadingIndicator
import filey.app.core.ui.components.LoadingIndicator
import filey.app.core.ui.components.PermissionScreen
import filey.app.core.ui.components.PermissionScreen
import filey.app.core.util.PermissionHelper
import filey.app.core.util.PermissionHelper
import filey.app.feature.browser.components.*
import filey.app.feature.browser.components.*


@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Composable
fun BrowserScreen(
fun BrowserScreen(
    initialPath: String = "/storage/emulated/0",
    initialPath: String = "/storage/emulated/0",
    onNavigate: (String) -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onOpenImage: (String) -> Unit = {},
    onOpenImage: (String) -> Unit = {},
    onOpenVideo: (String) -> Unit = {},
    onOpenVideo: (String) -> Unit = {},
    onOpenAudio: (String) -> Unit = {},
    onOpenAudio: (String) -> Unit = {},
    onOpenText: (String) -> Unit = {},
    onOpenText: (String) -> Unit = {},
    onOpenArchive: (String) -> Unit = {},
    onOpenArchive: (String) -> Unit = {},
    onBack: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    viewModel: BrowserViewModel = viewModel()
    viewModel: BrowserViewModel = viewModel()
) {
) {
    val context = LocalContext.current
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var hasPermission by remember { mutableStateOf(PermissionHelper.hasStoragePermission(context)) }
    var hasPermission by remember { mutableStateOf(PermissionHelper.hasStoragePermission(context)) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var selectedFileIndex by remember { mutableIntStateOf(-1) }
    var selectedFileIndex by remember { mutableIntStateOf(-1) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }


    val permissionLauncher = rememberLauncherForActivityResult(
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
        ActivityResultContracts.StartActivityForResult()
    ) {
    ) {
        hasPermission = PermissionHelper.hasStoragePermission(context)
        hasPermission = PermissionHelper.hasStoragePermission(context)
    }
    }


    LaunchedEffect(hasPermission) {
    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.loadDirectory(initialPath)
        if (hasPermission) viewModel.loadDirectory(initialPath)
    }
    }


    if (!hasPermission) {
    if (!hasPermission) {
        PermissionScreen(onRequestPermission = {
        PermissionScreen(onRequestPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    data = Uri.parse("package:${context.packageName}")
                }
                }
                permissionLauncher.launch(intent)
                permissionLauncher.launch(intent)
            }
            }
        })
        })
        return
        return
    }
    }


    Scaffold(
    Scaffold(
        topBar = {
        topBar = {
            if (state.isSearching) {
            if (state.isSearching) {
                SearchBar(
                SearchBar(
                    query = state.searchQuery,
                    query = state.searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onClose = { viewModel.toggleSearch() }
                    onClose = { viewModel.toggleSearch() }
                )
                )
            } else {
            } else {
                TopAppBar(
                TopAppBar(
                    title = {
                    title = {
                        Text(
                        Text(
                            text = state.currentPath.substringAfterLast("/").ifEmpty { "/" },
                            text = state.currentPath.substringAfterLast("/").ifEmpty { "/" },
                            maxLines = 1
                            maxLines = 1
                        )
                        )
                    },
                    },
                    navigationIcon = {
                    navigationIcon = {
                        if (onBack != null) {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                            }
                            }
                        }
                        }
                    },
                    },
                    actions = {
                    actions = {
                        IconButton(onClick = { showAboutDialog = true }) {
                        IconButton(onClick = { showAboutDialog = true }) {
                            Icon(Icons.Default.Info, "Hakkında")
                            Icon(Icons.Default.Info, "Hakkında")
                        }
                        }
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.Search, "Ara")
                            Icon(Icons.Default.Search, "Ara")
                        }
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                            Icon(
                                if (state.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                if (state.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                "Görünüm"
                                "Görünüm"
                            )
                            )
                        }
                        }
                        IconButton(onClick = { showSortSheet = true }) {
                        IconButton(onClick = { showSortSheet = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, "Sırala")
                            Icon(Icons.AutoMirrored.Filled.Sort, "Sırala")
                        }
                        }
                        IconButton(onClick = { showNewFolderDialog = true }) {
                        IconButton(onClick = { showNewFolderDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, "Yeni Klasör")
                            Icon(Icons.Default.CreateNewFolder, "Yeni Klasör")
                        }
                        }
                        if (state.clipboard != null) {
                        if (state.clipboard != null) {
                            IconButton(onClick = { viewModel.paste() }) {
                            IconButton(onClick = { viewModel.paste() }) {
                                Icon(Icons.Default.ContentPaste, "Yapıştır")
                                Icon(Icons.Default.ContentPaste, "Yapıştır")
                            }
                            }
                        }
                        }
                    }
                    }
                )
                )
            }
            }
        }
        }
    ) { padding ->
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                PathBar(
                PathBar(
                    path = state.currentPath,
                    path = state.currentPath,
                    onPathClick = { onNavigate(it) }
                    onPathClick = { onNavigate(it) }
                )
                )


                when {
                when {
                    state.isLoading -> LoadingIndicator()
                    state.isLoading -> LoadingIndicator()
                    state.files.isEmpty() -> EmptyState(message = "Bu klasör boş")
                    state.files.isEmpty() -> EmptyState(message = "Bu klasör boş")
                    state.isGridView -> FileGrid(
                    state.isGridView -> FileGrid(
                        files = state.files,
                        files = state.files,
                        onClick = { file ->
                        onClick = { file ->
                            when {
                            when {
                                file.isDirectory -> onNavigate(file.path)
                                file.isDirectory -> onNavigate(file.path)
                                file.type == FileType.IMAGE -> onOpenImage(file.path)
                                file.type == FileType.IMAGE -> onOpenImage(file.path)
                                file.type == FileType.VIDEO -> onOpenVideo(file.path)
                                file.type == FileType.VIDEO -> onOpenVideo(file.path)
                                file.type == FileType.AUDIO -> onOpenAudio(file.path)
                                file.type == FileType.AUDIO -> onOpenAudio(file.path)
                                file.type == FileType.ARCHIVE -> onOpenArchive(file.path)
                                file.type == FileType.ARCHIVE -> onOpenArchive(file.path)
                                file.type == FileType.TEXT -> onOpenText(file.path)
                                file.type == FileType.TEXT -> onOpenText(file.path)
                                else -> {
                                else -> {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            context,
                                            "${context.packageName}.fileprovider",
                                            "${context.packageName}.fileprovider",
                                            java.io.File(file.path)
                                            java.io.File(file.path)
                                        )
                                        )
                                        setDataAndType(uri, filey.app.core.util.FileUtils.getMimeType(file.path))
                                        setDataAndType(uri, filey.app.core.util.FileUtils.getMimeType(file.path))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    }
                                    try {
                                    try {
                                        context.startActivity(intent)
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                    } catch (_: Exception) {
                                        onOpenText(file.path)
                                        onOpenText(file.path)
                                    }
                                    }
                                }
                                }


                            }
                            }
                        },
                        },
                        onLongClick = { index ->
                        onLongClick = { index ->
                            selectedFileIndex = index
                            selectedFileIndex = index
                            showOptionsSheet = true
                            showOptionsSheet = true
                        }
                        }
                    )
                    )
                    else -> FileList(
                    else -> FileList(
                        files = state.files,
                        files = state.files,
                        onClick = { file ->
                        onClick = { file ->
                            when {
                            when {
                                file.isDirectory -> onNavigate(file.path)
                                file.isDirectory -> onNavigate(file.path)
                                file.type == FileType.IMAGE -> onOpenImage(file.path)
                                file.type == FileType.IMAGE -> onOpenImage(file.path)
                                file.type == FileType.VIDEO -> onOpenVideo(file.path)
                                file.type == FileType.VIDEO -> onOpenVideo(file.path)
                                file.type == FileType.AUDIO -> onOpenAudio(file.path)
                                file.type == FileType.AUDIO -> onOpenAudio(file.path)
                                file.type == FileType.ARCHIVE -> onOpenArchive(file.path)
                                file.type == FileType.ARCHIVE -> onOpenArchive(file.path)
                                file.type == FileType.TEXT -> onOpenText(file.path)
                                file.type == FileType.TEXT -> onOpenText(file.path)
                                else -> {
                                else -> {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            context,
                                            "${context.packageName}.fileprovider",
                                            "${context.packageName}.fileprovider",
                                            java.io.File(file.path)
                                            java.io.File(file.path)
                                        )
                                        )
                                        setDataAndType(uri, filey.app.core.util.FileUtils.getMimeType(file.path))
                                        setDataAndType(uri, filey.app.core.util.FileUtils.getMimeType(file.path))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    }
                                    try {
                                    try {
                                        context.startActivity(intent)
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                    } catch (_: Exception) {
                                        onOpenText(file.path)
                                        onOpenText(file.path)
                                    }
                                    }
                                }
                                }


                            }
                            }
                        },
                        },
                        onLongClick = { index ->
                        onLongClick = { index ->
                            selectedFileIndex = index
                            selectedFileIndex = index
                            showOptionsSheet = true
                            showOptionsSheet = true
                        }
                        }
                    )
                    )
                }
                }
            }
            }


            // İlerleme Kartı
            // İlerleme Kartı
            state.operationProgress?.let { progress ->
            state.operationProgress?.let { progress ->
                OperationProgressCard(
                OperationProgressCard(
                    fileName = state.clipboard?.file?.name ?: "Dosya",
                    fileName = state.clipboard?.file?.name ?: "Dosya",
                    progress = progress,
                    progress = progress,
                    modifier = Modifier.align(Alignment.BottomCenter)
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                )
            }
            }
        }
        }
    }
    }


    if (showSortSheet) {
    if (showSortSheet) {
        SortSheet(
        SortSheet(
            currentSort = state.sortOption,
            currentSort = state.sortOption,
            showHidden = state.showHidden,
            showHidden = state.showHidden,
            onSortSelected = { viewModel.setSortOption(it) },
            onSortSelected = { viewModel.setSortOption(it) },
            onToggleHidden = { viewModel.toggleShowHidden() },
            onToggleHidden = { viewModel.toggleShowHidden() },
            onDismiss = { showSortSheet = false }
            onDismiss = { showSortSheet = false }
        )
        )
    }
    }


    if (showOptionsSheet && selectedFileIndex in state.files.indices) {
    if (showOptionsSheet && selectedFileIndex in state.files.indices) {
        val file = state.files[selectedFileIndex]
        val file = state.files[selectedFileIndex]
        FileOptionsSheet(
        FileOptionsSheet(
            file = file,
            file = file,
            onCopy = { viewModel.copyFile(file); showOptionsSheet = false },
            onCopy = { viewModel.copyFile(file); showOptionsSheet = false },
            onCut = { viewModel.cutFile(file); showOptionsSheet = false },
            onCut = { viewModel.cutFile(file); showOptionsSheet = false },
            onRename = { showOptionsSheet = false; showRenameDialog = true },
            onRename = { showOptionsSheet = false; showRenameDialog = true },
            onDelete = { showOptionsSheet = false; showDeleteDialog = true },
            onDelete = { showOptionsSheet = false; showDeleteDialog = true },
            onDismiss = { showOptionsSheet = false }
            onDismiss = { showOptionsSheet = false }
        )
        )
    }
    }


    if (showNewFolderDialog) {
    if (showNewFolderDialog) {
        var name by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        AlertDialog(
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Yeni Klasör") },
            title = { Text("Yeni Klasör") },
            text = {
            text = {
                OutlinedTextField(
                OutlinedTextField(
                    value = name,
                    value = name,
                    onValueChange = { name = it },
                    onValueChange = { name = it },
                    label = { Text("Klasör adı") },
                    label = { Text("Klasör adı") },
                    singleLine = true
                    singleLine = true
                )
                )
            },
            },
            confirmButton = {
            confirmButton = {
                TextButton(onClick = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                    if (name.isNotBlank()) {
                        viewModel.createFolder(name)
                        viewModel.createFolder(name)
                        showNewFolderDialog = false
                        showNewFolderDialog = false
                    }
                    }
                }) { Text("Oluştur") }
                }) { Text("Oluştur") }
            },
            },
            dismissButton = {
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("İptal") }
                TextButton(onClick = { showNewFolderDialog = false }) { Text("İptal") }
            }
            }
        )
        )
    }
    }


    if (showRenameDialog && selectedFileIndex in state.files.indices) {
    if (showRenameDialog && selectedFileIndex in state.files.indices) {
        val file = state.files[selectedFileIndex]
        val file = state.files[selectedFileIndex]
        var newName by remember { mutableStateOf(file.name) }
        var newName by remember { mutableStateOf(file.name) }
        AlertDialog(
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Yeniden Adlandır") },
            title = { Text("Yeniden Adlandır") },
            text = {
            text = {
                OutlinedTextField(
                OutlinedTextField(
                    value = newName,
                    value = newName,
                    onValueChange = { newName = it },
                    onValueChange = { newName = it },
                    label = { Text("Yeni ad") },
                    label = { Text("Yeni ad") },
                    singleLine = true
                    singleLine = true
                )
                )
            },
            },
            confirmButton = {
            confirmButton = {
                TextButton(onClick = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                    if (newName.isNotBlank()) {
                        viewModel.renameFile(file, newName)
                        viewModel.renameFile(file, newName)
                        showRenameDialog = false
                        showRenameDialog = false
                    }
                    }
                }) { Text("Kaydet") }
                }) { Text("Kaydet") }
            },
            },
            dismissButton = {
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("İptal") }
                TextButton(onClick = { showRenameDialog = false }) { Text("İptal") }
            }
            }
        )
        )
    }
    }


    if (showDeleteDialog && selectedFileIndex in state.files.indices) {
    if (showDeleteDialog && selectedFileIndex in state.files.indices) {
        val file = state.files[selectedFileIndex]
        val file = state.files[selectedFileIndex]
        AlertDialog(
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Sil") },
            title = { Text("Sil") },
            text = { Text("${file.name} silinsin mi?") },
            text = { Text("${file.name} silinsin mi?") },
            confirmButton = {
            confirmButton = {
                TextButton(onClick = {
                TextButton(onClick = {
                    viewModel.deleteFile(file)
                    viewModel.deleteFile(file)
                    showDeleteDialog = false
                    showDeleteDialog = false
                }) { Text("Sil") }
                }) { Text("Sil") }
            },
            },
            dismissButton = {
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("İptal") }
                TextButton(onClick = { showDeleteDialog = false }) { Text("İptal") }
            }
            }
        )
        )
    }
    }


    if (showAboutDialog) {
    if (showAboutDialog) {
        AlertDialog(
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            onDismissRequest = { showAboutDialog = false },
            title = {
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                    Icon(
                        Icons.Default.Info,
                        Icons.Default.Info,
                        contentDescription = null,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                        modifier = Modifier.size(32.dp)
                    )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Filey Hakkında")
                    Text("Filey Hakkında")
                }
                }
            },
            },
            text = {
            text = {
                Column {
                Column {
                    Text(
                    Text(
                        "Filey - Modern Dosya Yöneticisi",
                        "Filey - Modern Dosya Yöneticisi",
                        style = MaterialTheme.typography.titleSmall,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                        fontWeight = FontWeight.Bold
                    )
                    )
                    Text("Sürüm: 1.0.0", style = MaterialTheme.typography.bodySmall)
                    Text("Sürüm: 1.0.0", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                    Text(
                        "Jetpack Compose ve Material 3 ile geliştirilmiş, Root desteğine sahip hızlı bir dosya yöneticisi.",
                        "Jetpack Compose ve Material 3 ile geliştirilmiş, Root desteğine sahip hızlı bir dosya yöneticisi.",
                        style = MaterialTheme.typography.bodyMedium
                        style = MaterialTheme.typography.bodyMedium
                    )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                    Text(
                        "Geliştirici: Ömer Süsin",
                        "Geliştirici: Ömer Süsin",
                        style = MaterialTheme.typography.bodySmall,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                        color = MaterialTheme.colorScheme.primary
                    )
                    )
                }
                }
            },
            },
            confirmButton = {
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Kapat")
                    Text("Kapat")
                }
                }
            }
            }
        )
        )
    }
    }
}
}


@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Composable
private fun SearchBar(
private fun SearchBar(
    query: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
    onClose: () -> Unit
) {
) {
    TopAppBar(
    TopAppBar(
        title = {
        title = {
            OutlinedTextField(
            OutlinedTextField(
                value = query,
                value = query,
                onValueChange = onQueryChange,
                onValueChange = onQueryChange,
                placeholder = { Text("Dosya ara...") },
                placeholder = { Text("Dosya ara...") },
                singleLine = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
                )
            )
            )
        },
        },
        navigationIcon = {
        navigationIcon = {
            IconButton(onClick = onClose) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kapat")
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kapat")
            }
            }
        }
        }
    )
    )
}
}
