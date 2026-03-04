package filey.app.feature.analyzer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import filey.app.core.model.FileCategory
import filey.app.core.model.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAnalyzerScreen(
    onBack: () -> Unit,
    viewModel: StorageAnalyzerViewModel = viewModel(factory = StorageAnalyzerViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Depolama Analizi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main storage overview
                item {
                    StorageSummaryCard(uiState)
                }

                item {
                    Text(
                        "Kategori Detayları",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(FileCategory.entries) { category ->
                    val size = uiState.categorySizes[category] ?: 0L
                    CategoryUsageRow(category, size, uiState.storageInfo?.totalBytes ?: 1L)
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "En Büyük Dosyalar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.largestFiles) { file ->
                    ListItem(
                        headlineContent = { Text(file.name, maxLines = 1) },
                        supportingContent = { Text(file.path, maxLines = 1) },
                        trailingContent = {
                            Text(
                                FileUtils.formatSize(file.size),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun StorageSummaryCard(uiState: AnalyzerUiState) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            uiState.storageInfo?.let { info ->
                Text("Kullanılan Alan", style = MaterialTheme.typography.labelMedium)
                Text(
                    FileUtils.formatSize(info.usedBytes),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${FileUtils.formatSize(info.freeBytes)} boş alan (Toplam ${FileUtils.formatSize(info.totalBytes)})",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(Modifier.height(16.dp))
                
                val progress = if (info.totalBytes > 0) info.usedBytes.toFloat() / info.totalBytes else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}

@Composable
fun CategoryUsageRow(category: FileCategory, size: Long, total: Long) {
    val percentage = (size.toFloat() / total * 100).coerceAtLeast(0.1f)
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(category.icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(category.label, style = MaterialTheme.typography.bodyMedium)
            }
            Text(FileUtils.formatSize(size), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { size.toFloat() / total },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
