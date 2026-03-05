package filey.app.feature.analyzer

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                title = { Text("Depolama Analizi", fontWeight = FontWeight.Bold) },
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
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    StoragePieChartSection(uiState)
                }

                item {
                    Text(
                        "Kategorilere Göre Dağılım",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                items(FileCategory.entries) { category ->
                    val size = uiState.categorySizes[category] ?: 0L
                    CategoryDetailCard(category, size, uiState.storageInfo?.usedBytes ?: 1L)
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "En Büyük 10 Dosya",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                items(uiState.largestFiles) { file ->
                    LargeFileItem(file)
                }
                
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun StoragePieChartSection(uiState: AnalyzerUiState) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val usedBytes = uiState.storageInfo?.usedBytes ?: 0L
            val totalBytes = uiState.storageInfo?.totalBytes ?: 1L
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                StoragePieChart(uiState.categorySizes, usedBytes)
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${((usedBytes.toFloat() / totalBytes) * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Dolu",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StorageStatItem("Kullanılan", FileUtils.formatSize(usedBytes), MaterialTheme.colorScheme.primary)
                StorageStatItem("Boş", FileUtils.formatSize(uiState.storageInfo?.freeBytes ?: 0L), MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun StoragePieChart(categorySizes: Map<FileCategory, Long>, totalUsed: Long) {
    val categories = FileCategory.entries
    val colors = listOf(
        Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFF9C27B0),
        Color(0xFFFF9800), Color(0xFF4CAF50), Color(0xFFF44336), Color(0xFF607D8B)
    )

    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        var startAngle = -90f
        
        categorySizes.entries.forEachIndexed { index, entry ->
            val sweepAngle = (entry.value.toFloat() / totalUsed.coerceAtLeast(1L)) * 360f
            if (sweepAngle > 0.5f) {
                drawArc(
                    color = colors.getOrElse(index) { Color.Gray },
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                )
                startAngle += sweepAngle
            }
        }
        
        // Background track for empty space if any
        if (startAngle < 270f) {
            drawArc(
                color = Color.LightGray.copy(alpha = 0.2f),
                startAngle = startAngle,
                sweepAngle = 270f - startAngle,
                useCenter = false,
                style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun StorageStatItem(label: String, value: String, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CategoryDetailCard(category: FileCategory, size: Long, totalUsed: Long) {
    val percentage = (size.toFloat() / totalUsed * 100).coerceAtLeast(0f)
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(category.icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(category.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${FileUtils.formatSize(size)} • ${"%.1f".format(percentage)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
fun LargeFileItem(file: filey.app.core.model.FileModel) {
    ListItem(
        headlineContent = { Text(file.name, maxLines = 1, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(file.path, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
        trailingContent = {
            Text(
                FileUtils.formatSize(file.size),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
