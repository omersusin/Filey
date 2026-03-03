package filey.app.feature.browser.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import filey.app.core.model.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortSheet(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Sıralama",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        SortOption.entries.forEach { option ->
            ListItem(
                modifier = Modifier.clickable { onSortSelected(option) },
                headlineContent = { Text(option.label) },
                trailingContent = {
                    if (option == currentSort) {
                        Icon(Icons.Default.Check, "Seçili", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    }
}
