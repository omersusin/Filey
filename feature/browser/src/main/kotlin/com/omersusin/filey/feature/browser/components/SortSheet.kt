package com.omersusin.filey.feature.browser.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omersusin.filey.core.model.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortSheet(
    currentSort: SortOption,
    showHidden: Boolean,
    onSortSelected: (SortOption) -> Unit,
    onToggleHidden: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "Sıralama",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            SortOption.entries.forEach { option ->
                ListItem(
                    headlineContent = {
                        Text(
                            when (option) {
                                SortOption.NAME_ASC -> "Ad (A-Z)"
                                SortOption.NAME_DESC -> "Ad (Z-A)"
                                SortOption.DATE_ASC -> "Tarih (Eski)"
                                SortOption.DATE_DESC -> "Tarih (Yeni)"
                                SortOption.SIZE_ASC -> "Boyut (Küçük)"
                                SortOption.SIZE_DESC -> "Boyut (Büyük)"
                                SortOption.TYPE_ASC -> "Tür (A-Z)"
                                SortOption.TYPE_DESC -> "Tür (Z-A)"
                            }
                        )
                    },
                    leadingContent = {
                        RadioButton(
                            selected = currentSort == option,
                            onClick = { onSortSelected(option) }
                        )
                    },
                    modifier = Modifier.clickable { onSortSelected(option) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text("Gizli dosyaları göster") },
                trailingContent = {
                    Switch(checked = showHidden, onCheckedChange = { onToggleHidden() })
                }
            )
        }
    }
}
