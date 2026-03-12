package filey.app.feature.browser

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
    onNavigateToPdf: (String) -> Unit,
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

    // ── Permission Management ──
    var hasPermission by remember { 
        mutableStateOf(checkStoragePermission(context)) 
    }
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

    // Snackbar & Error Handling
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

    // Navigation Drawer State
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            BrowserDrawerContent(
                uiState = uiState,
                onDashboard = onNavigateToDashboard,
                onTrash = onNavigateToTrash,
                onServer = onNavigateToServer,
                onSettings = onNavigateToSettings,
                onNavigate = { path ->
                    viewModel.navigateTo(path)
                    scope.launch { drawerState.close() }
                },
                onFileClick = { file ->
                    handleFileClick(
                        file = file,
                        viewModel = viewModel,
                        onImage = onNavigateToImage,
                        onVideo = onNavigateToVideo,
                        onAudio = onNavigateToAudio,
                        onText = onNavigateToEditor,
                        onArchive = onNavigateToArchive,
                        onPdf = onNavigateToPdf,
                        context = context,
                        callback = actionCallback

                    )
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        // ... (Scaffold structure will be optimized next)
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                BrowserTopBar(
                    uiState = uiState,
                    viewModel = viewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigateToDashboard = onNavigateToDashboard,
                    onSettings = onNavigateToSettings
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Shelf / Staging Area
                AnimatedVisibility(
                    visible = uiState.shelf.isNotEmpty(),
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    ShelfBar(
                        count = uiState.shelf.size,
                        onClear = { viewModel.clearShelf() },
                        onPaste = { viewModel.pasteFromShelf() }
                    )
                }

                // PathBar with breadcrumbs
                PathBar(
                    segments = uiState.pathSegments,
                    onSegmentClick = { segment -> viewModel.navigateTo(segment.fullPath) }
                )

                // Operation progress indicator
                if (uiState.operationMessage != null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = uiState.operationMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Main Content
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        !hasPermission -> PermissionRequiredContent { 
                            permissionLauncher.launch(storagePermissions()) 
                        }
                        uiState.isLoading -> LoadingContent()
                        uiState.error != null && uiState.files.isEmpty() -> ErrorContent(uiState.error!!) {
                            viewModel.refreshCurrentDirectory()
                        }
                        uiState.displayFiles.isEmpty() -> EmptyContent(uiState.isSearchActive)
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
                                            onPdf = onNavigateToPdf,
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
                                            // Handle file options
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShelfBar(count: Int, onClear: () -> Unit, onPaste: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(16.dp))
            Text(
                "Rafta $count öğe var",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClear) {
                Text("Temizle", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Button(
                onClick = onPaste,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Buraya Koy")
            }
        }
    }
}

// ── Helper Functions for Permissions ──

private fun checkStoragePermission(context: android.content.Context): Boolean {
    val permissions = storagePermissions()
    return permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
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

private fun handleFileClick(
    file: FileModel,
    viewModel: BrowserViewModel,
    onImage: (String) -> Unit,
    onVideo: (String) -> Unit,
    onAudio: (String) -> Unit,
    onText: (String) -> Unit,
    onArchive: (String) -> Unit,
    onPdf: (String) -> Unit,
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
        FileType.PDF -> onPdf(file.path)
        else -> {
            scope.launch {
                OpenWithAction().execute(context, file, callback)
            }
        }
    }
}

@Composable
fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 3.dp)
    }
}

@Composable
fun EmptyContent(isSearch: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isSearch) Icons.Default.SearchOff else Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isSearch) "Sonuç bulunamadı" else "Bu klasör boş",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
