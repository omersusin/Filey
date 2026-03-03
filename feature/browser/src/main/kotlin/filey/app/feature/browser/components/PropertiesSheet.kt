package filey.app.feature.browser.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import filey.app.core.model.FileModel
import filey.app.core.model.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesSheet(
    file: FileModel,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
            Text("Dosya Özellikleri", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            PropertyRow("Ad", file.name)
            PropertyRow("Yol", file.path)
            PropertyRow("Tür", if (file.isDirectory) "Klasör" else file.mimeType)
            if (!file.isDirectory) {
                PropertyRow("Boyut", FileUtils.formatSize(file.size))
                PropertyRow("Uzantı", file.extension.ifEmpty { "-" })
            }
            PropertyRow("Son değişiklik", FileUtils.formatDate(file.lastModified))
            if (file.permissions.isNotBlank()) {
                PropertyRow("İzinler", file.permissions)
            }
            if (file.owner.isNotBlank()) {
                PropertyRow("Sahip", file.owner)
            }
            PropertyRow("Gizli", if (file.isHidden) "Evet" else "Hayır")
        }
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
