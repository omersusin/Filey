package filey.app.feature.browser.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import filey.app.core.model.FileType
import filey.app.feature.browser.SearchFilters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchFilterSheet(
    filters: SearchFilters,
    onFiltersChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Arama Filtreleri", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))

            // File Type
            Text("Dosya Türü", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FileType.entries.filter { it != FileType.DIRECTORY }.forEach { type ->
                    FilterChip(
                        selected = filters.type == type,
                        onClick = {
                            onFiltersChange(filters.copy(type = if (filters.type == type) null else type))
                        },
                        label = { Text(type.name) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Size
            Text("Minimum Boyut", style = MaterialTheme.typography.labelLarge)
            val sizes = listOf(
                0L to "Herhangi",
                1024 * 1024L to "1 MB+",
                10 * 1024 * 1024L to "10 MB+",
                100 * 1024 * 1024L to "100 MB+",
                1024 * 1024 * 1024L to "1 GB+"
            )
            FlowRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sizes.forEach { (bytes, label) ->
                    FilterChip(
                        selected = filters.minSize == bytes,
                        onClick = { onFiltersChange(filters.copy(minSize = bytes)) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Date
            Text("Zaman Aralığı", style = MaterialTheme.typography.labelLarge)
            val now = System.currentTimeMillis()
            val day = 24 * 60 * 60 * 1000L
            val dates = listOf(
                0L to "Her zaman",
                now - day to "Son 24 saat",
                now - (7 * day) to "Son 1 hafta",
                now - (30 * day) to "Son 1 ay"
            )
            FlowRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dates.forEach { (time, label) ->
                    FilterChip(
                        selected = filters.dateAfter == time,
                        onClick = { onFiltersChange(filters.copy(dateAfter = time)) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Uygula")
            }
        }
    }
}
