package filey.app.feature.browser

import android.Manifest
import android.os.Build
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import filey.app.core.model.*
import filey.app.feature.browser.actions.ActionRegistry
import filey.app.feature.browser.actions.ActionResult
import filey.app.feature.browser.actions.OpenWithAction
import filey.app.feature.browser.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    onNavigateToImage: (String) -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToAudio: (String) -> Unit,
    onNavigateToEditor: (String) -> Unit,
    onNavigateToArchive: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToServer: () -> Unit,
    viewModel: BrowserViewModel = viewModel(factory = BrowserViewModel.Factory)
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarMessage by viewModel.snackbarEvent.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val actionRegistry = remember { ActionRegistry.createDefault() }
    val actionCallback = remember(viewModel, context) { viewModel.createActionCallback(context) }

    // ── Permission ──
    var hasPermission by remember { mutableStateOf(checkStoragePermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasPermission = grants.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(storagePermissions())
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.refreshCurrentDirectory()
        }
    }

    // Snackbar event
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { data ->
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = data.message,
                    actionLabel = data.actionLabel,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    data.onAction?.invoke()
                }
                viewModel.clearSnackbar()
            }
        }
    }

    // Error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(
                message = err,
                actionLabel = "Kapat",
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    // Back handling
    BackHandler {
        when {
            uiState.isSearchActive -> viewModel.toggleSearch()
            uiState.isMultiSelectActive -> viewModel.clearSelection()
            else -> viewModel.goBack()
        }
    }

    // ── Dialog/sheet states ──
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showOptionsFor by remember { mutableStateOf<FileModel?>(null) }
    var showRenameFor by remember { mutableStateOf<FileModel?>(null) }
    var showBatchRenameDialog by remember { mutableStateOf(false) }
    var showSearchFilterSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmFor by remember { mutableStateOf<FileModel?>(null) }
    var showPropertiesFor by remember { mutableStateOf<FileModel?>(null) }
    var showDeleteSelectedConfirm by remember { mutableStateOf(false) }
    var showAccessModeSheet by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))

                NavigationDrawerItem(
                    label = { Text("Ana Sayfa") },
                    selected = false,
                    onClick = {
                        onNavigateToDashboard()
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Home, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Çöp Kutusu") },
                    selected = false,
                    onClick = {
                        onNavigateToTrash()
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Delete, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("Dosya Paylaşımı") },
                    selected = false,
                    onClick = {
                        onNavigateToServer()
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Language, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                
                // Storage Info
                uiState.storageInfo?.let { info ->
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hafıza", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${FileUtils.formatSize(info.usedBytes)} / ${FileUtils.formatSize(info.totalBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        val progress = if (info.totalBytes > 0) info.usedBytes.toFloat() / info.totalBytes else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (progress > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${FileUtils.formatSize(info.freeBytes)} boş alan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Text(
                        "Favori Klasörler",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (uiState.favorites.isEmpty()) {
                        Text(
                            "Henüz favori yok",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.favorites.toList().sorted().forEach { favPath ->
                            NavigationDrawerItem(
                                label = { Text(favPath.substringAfterLast('/')) },
                                selected = uiState.currentPath == favPath,
                                onClick = {
                                    viewModel.navigateTo(favPath)
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(Icons.Default.Folder, null) },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        "Son Kullanılanlar",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (uiState.recents.isEmpty()) {
                        Text(
                            "Henüz kayıt yok",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.recents.forEach { recentPath ->
                            NavigationDrawerItem(
                                label = {
                                    Text(
                                        recentPath.substringAfterLast('/'),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                selected = false,
                                onClick = {
                                    scope.launch {
                                        drawerState.close()
                                        // Open the file
                                        val file = try {
                                            viewModel.uiState.value.files.find { it.path == recentPath } 
                                                ?: FileModel(
                                                    name = recentPath.substringAfterLast('/'),
                                                    path = recentPath,
                                                    isDirectory = false
                                                )
                                        } catch (e: Exception) {
                                            FileModel(recentPath.substringAfterLast('/'), recentPath, false)
                                        }
                                        handleFileClick(
                                            file = file,
                                            viewModel = viewModel,
                                            onImage = onNavigateToImage,
                                            onVideo = onNavigateToVideo,
                                            onAudio = onNavigateToAudio,
                                            onText = onNavigateToEditor,
                                            onArchive = onNavigateToArchive,
                                            context = context,
                                            callback = actionCallback
                                        )
                                    }
                                },
                                icon = {
                                    Icon(
                                        when (FileUtils.getFileType(recentPath, false)) {
                                            FileType.IMAGE -> Icons.Outlined.Image
                                            FileType.VIDEO -> Icons.Outlined.Movie
                                            FileType.AUDIO -> Icons.Outlined.MusicNote
                                            else -> Icons.Outlined.InsertDriveFile
                                        },
                                        null
                                    )
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                }
                
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Ayarlar") },
                    selected = false,
                    onClick = {
                        onNavigateToSettings()
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Settings, null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                when {
                    uiState.isMultiSelectActive -> {
                        MultiSelectTopBar(
                            selectedCount = uiState.selectedFiles.size,
                            onClose = { viewModel.clearSelection() },
                            onSelectAll = { viewModel.selectAll() },
                            onDeleteSelected = { showDeleteSelectedConfirm = true },
                            onRenameSelected = { showBatchRenameDialog = true },
                            onCopySelected = {
                                viewModel.setClipboard(
                                    uiState.selectedFiles.toList(), isCut = false
                                )
                                viewModel.clearSelection()
                                viewModel.showSnackbar("Kopyalandı")
                            },
                            onCutSelected = {
                                viewModel.setClipboard(
                                    uiState.selectedFiles.toList(), isCut = true
                                )
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
                            onFilterClick = { showSearchFilterSheet = true },
                            onClose = { viewModel.toggleSearch() }
                        )
                    }
                    else -> {
                        TopAppBar(
                            title = {
                                Text(
                                    text = uiState.pathSegments.lastOrNull()?.name ?: "Filey",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            navigationIcon = {
                                if (uiState.canGoBack) {
                                    IconButton(onClick = { viewModel.goBack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                                    }
                                } else if (uiState.currentPath != BrowserUiState.DEFAULT_PATH &&
                                    uiState.pathSegments.size <= 1) {
                                    // Categories etc
                                    IconButton(onClick = onNavigateToDashboard) {
                                        Icon(Icons.Default.Home, "Ana Sayfa")
                                    }
                                } else if (uiState.currentPath != BrowserUiState.DEFAULT_PATH) {
                                    IconButton(onClick = { viewModel.navigateUp() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                                    }
                                } else {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, "Menü")
                                    }
                                }
                            },
                            actions = {
                                if (uiState.accessMode != AccessMode.NORMAL) {
                                    IconButton(onClick = { showAccessModeSheet = true }) {
                                        Icon(
                                            imageVector = when (uiState.accessMode) {
                                                AccessMode.ROOT -> Icons.Default.Security
                                                AccessMode.SHIZUKU -> Icons.Default.Shield
                                                else -> Icons.Default.Lock
                                            },
                                            contentDescription = uiState.accessMode.name,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                IconButton(onClick = { viewModel.toggleSearch() }) {
                                    Icon(Icons.Default.Search, "Ara")
                                }

                                IconButton(onClick = {
                                    val newMode = if (uiState.viewMode == ViewMode.LIST)
                                        ViewMode.GRID else ViewMode.LIST
                                    viewModel.setViewMode(newMode)
                                }) {
                                    Icon(
                                        if (uiState.viewMode == ViewMode.LIST) Icons.Default.GridView
                                        else Icons.AutoMirrored.Filled.ViewList,
                                        "Görünüm"
                                    )
                                }

                                IconButton(onClick = { showSortSheet = true }) {
                                    Icon(Icons.Default.Sort, "Sırala")
                                }

                                IconButton(onClick = { showCreateFolderDialog = true }) {
                                    Icon(Icons.Default.CreateNewFolder, "Yeni Klasör")
                                }

                                if (uiState.clipboard != null) {
                                    IconButton(onClick = { viewModel.paste() }) {
                                        Icon(
                                            Icons.Default.ContentPaste, "Yapıştır",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Overflow
                                var expanded by remember { mutableStateOf(false) }
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.MoreVert, "Daha fazla")
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (uiState.showHiddenFiles) "Gizlileri gizle"
                                                else "Gizlileri göster"
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                if (uiState.showHiddenFiles) Icons.Default.VisibilityOff
                                                else Icons.Default.Visibility,
                                                null
                                            )
                                        },
                                        onClick = {
                                            viewModel.toggleHiddenFiles()
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Erişim modu") },
                                        leadingIcon = { Icon(Icons.Default.Security, null) },
                                        onClick = {
                                            showAccessModeSheet = true
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Yenile") },
                                        leadingIcon = { Icon(Icons.Default.Refresh, null) },
                                        onClick = {
                                            viewModel.refreshCurrentDirectory()
                                            expanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Ayarlar") },
                                        leadingIcon = { Icon(Icons.Default.Settings, null) },
                                        onClick = {
                                            expanded = false
                                            onNavigateToSettings()
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Shelf Bar (Persistent staging area)
                AnimatedVisibility(visible = uiState.shelf.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Rafta ${uiState.shelf.size} öğe var",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearShelf() }) {
                                Text("Temizle", color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                            Button(
                                onClick = { viewModel.pasteFromShelf() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Text("Buraya Koy")
                            }
                        }
                    }
                }

                // PathBar
                if (!uiState.isSearchActive) {
                    PathBar(
                        segments = uiState.pathSegments,
                        onSegmentClick = { segment -> viewModel.navigateTo(segment.fullPath) }
                    )
                }

                // Operation progress
                AnimatedVisibility(visible = uiState.operationMessage != null) {
                    Column {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        uiState.operationMessage?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Content
                when {
                    !hasPermission -> {
                        PermissionRequiredContent(
                            onRequestPermission = { permissionLauncher.launch(storagePermissions()) }
                        )
                    }
                    uiState.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.error != null && uiState.files.isEmpty() -> {
                        ErrorContent(
                            message = uiState.error!!,
                            onRetry = { viewModel.refreshCurrentDirectory() }
                        )
                    }
                    uiState.displayFiles.isEmpty() -> {
                        EmptyContent(
                            isSearchActive = uiState.isSearchActive,
                            showHiddenFiles = uiState.showHiddenFiles,
                            onToggleHidden = { viewModel.toggleHiddenFiles() }
                        )
                    }
                    else -> {
                        FileContent(
                            files = uiState.displayFiles,
                            viewMode = uiState.viewMode,
                            selectedFiles = uiState.selectedFiles,
                            isMultiSelectActive = uiState.isMultiSelectActive,
                            onFileClick = { file ->
                                if (file.name == "^") {
                                    viewModel.navigateUp()
                                } else if (uiState.isMultiSelectActive) {
                                    viewModel.toggleFileSelection(file.path)
                                } else {
                                    handleFileClick(
                                        file = file,
                                        viewModel = viewModel,
                                        onImage = onNavigateToImage,
                                        onVideo = onNavigateToVideo,
                                        onAudio = onNavigateToAudio,
                                        onText = onNavigateToEditor,
                                        onArchive = onNavigateToArchive,
                                        context = context,
                                        callback = actionCallback
                                    )
                                }
                            },
                            onFileLongClick = { file ->
                                if (file.name != "^") {
                                    if (uiState.isMultiSelectActive) {
                                        viewModel.toggleFileSelection(file.path)
                                    } else {
                                        showOptionsFor = file
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // ══════════ DIALOGS & SHEETS ══════════

        if (showCreateFolderDialog) {
            CreateFolderDialog(
                onDismiss = { showCreateFolderDialog = false },
                onCreate = { name ->
                    viewModel.createFolder(name)
                    showCreateFolderDialog = false
                }
            )
        }

        if (showBatchRenameDialog) {
            BatchRenameDialog(
                selectedCount = uiState.selectedFiles.size,
                onDismiss = { showBatchRenameDialog = false },
                onRename = { base, prefix, suffix, start ->
                    viewModel.batchRename(base, prefix, suffix, start)
                    showBatchRenameDialog = false
                }
            )
        }

        if (showSearchFilterSheet) {
            SearchFilterSheet(
                filters = uiState.searchFilters,
                onFiltersChange = { viewModel.updateSearchFilters(it) },
                onDismiss = { showSearchFilterSheet = false }
            )
        }

        if (showSortSheet) {
            SortSheet(
                currentSort = uiState.sortOption,
                onSortSelected = { option ->
                    viewModel.setSortOption(option)
                    showSortSheet = false
                },
                onDismiss = { showSortSheet = false }
            )
        }

        showOptionsFor?.let { file ->
            FileOptionsSheet(
                file = file,
                actions = actionRegistry.getActionsForFile(file),
                favorites = uiState.favorites,
                shelf = uiState.shelf,
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onToggleShelf = { viewModel.toggleShelfItem(it) },
                callback = actionCallback,
                onResult = { result ->
                    showOptionsFor = null
                    when (result) {
                        is ActionResult.RequestDelete -> {
                            showDeleteConfirmFor = file
                        }
                        is ActionResult.RequestRename -> {
                            showRenameFor = file
                        }
                        is ActionResult.RequestProperties -> {
                            showPropertiesFor = file
                        }
                        is ActionResult.Error -> {
                            viewModel.showSnackbar(result.message)
                        }
                        is ActionResult.Success -> { /* handled by callback */ }
                        ActionResult.Dismissed -> { /* noop */ }
                    }
                },
                onDismiss = { showOptionsFor = null }
            )
        }

        showRenameFor?.let { file ->
            RenameDialog(
                currentName = file.name,
                onDismiss = { showRenameFor = null },
                onRename = { newName ->
                    viewModel.renameFile(file.path, newName)
                    showRenameFor = null
                }
            )
        }

        showDeleteConfirmFor?.let { file ->
            DeleteConfirmDialog(
                fileName = file.name,
                onDismiss = { showDeleteConfirmFor = null },
                onConfirm = {
                    viewModel.moveToTrash(file.path)
                    showDeleteConfirmFor = null
                }
            )
        }

        if (showDeleteSelectedConfirm) {
            DeleteConfirmDialog(
                fileName = "${uiState.selectedFiles.size} öğe",
                onDismiss = { showDeleteSelectedConfirm = false },
                onConfirm = {
                    viewModel.deleteSelected(permanently = false)
                    showDeleteSelectedConfirm = false
                }
            )
        }

        showPropertiesFor?.let { file ->
            PropertiesSheet(
                file = file,
                onDismiss = { showPropertiesFor = null }
            )
        }

        if (showAccessModeSheet) {
            AccessModeSheet(
                currentMode = uiState.accessMode,
                context = context,
                onModeSelected = { mode ->
                    viewModel.setAccessMode(mode)
                    showAccessModeSheet = false
                },
                onDismiss = { showAccessModeSheet = false }
            )
        }
    }
}

// ══════════ HELPERS ══════════

private fun handleFileClick(
    file: FileModel,
    viewModel: BrowserViewModel,
    onImage: (String) -> Unit,
    onVideo: (String) -> Unit,
    onAudio: (String) -> Unit,
    onText: (String) -> Unit,
    onArchive: (String) -> Unit,
    context: android.content.Context,
    callback: filey.app.feature.browser.actions.FileActionCallback
) {
    if (file.isDirectory) {
        viewModel.navigateTo(file.path)
        return
    }
    viewModel.addToRecents(file.path)
    val scope = kotlinx.coroutines.MainScope() 
    when (FileUtils.getFileType(file.path, false)) {
        FileType.IMAGE -> onImage(file.path)
        FileType.VIDEO -> onVideo(file.path)
        FileType.AUDIO -> onAudio(file.path)
        FileType.TEXT -> onText(file.path)
        FileType.ARCHIVE -> onArchive(file.path)
        else -> {
            scope.launch {
                OpenWithAction().execute(context, file, callback)
            }
        }
    }
}

private fun checkStoragePermission(): Boolean {
    return true 
}

private fun storagePermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}
