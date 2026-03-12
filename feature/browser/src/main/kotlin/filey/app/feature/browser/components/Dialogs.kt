package filey.app.feature.browser.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Klasör") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Klasör adı") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Oluştur")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}

@Composable
fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeniden Adlandır") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Yeni ad") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && name != currentName) onRename(name.trim())
                },
                enabled = name.isNotBlank() && name != currentName
            ) {
                Text("Yeniden Adlandır")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}

@Composable
fun BatchRenameDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onRename: (base: String, prefix: String, suffix: String, startNumber: Int) -> Unit
) {
    var baseName by remember { mutableStateOf("") }
    var prefix by remember { mutableStateOf("") }
    var suffix by remember { mutableStateOf("") }
    var startNumber by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Toplu Yeniden Adlandır ($selectedCount öğe)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = prefix,
                    onValueChange = { prefix = it },
                    label = { Text("Ön ek (Prefix)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = baseName,
                    onValueChange = { baseName = it },
                    label = { Text("Ana isim (Boş bırakılırsa orijinal ad korunur)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = suffix,
                    onValueChange = { suffix = it },
                    label = { Text("Son ek (Suffix)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = startNumber,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) startNumber = it },
                    label = { Text("Başlangıç sayısı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Örnek: ${prefix}${baseName.ifEmpty { "dosya" }}_001${suffix}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onRename(baseName, prefix, suffix, startNumber.toIntOrNull() ?: 1)
                }
            ) {
                Text("Uygula")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    fileName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Silme Onayı") },
        text = { Text("'$fileName' kalıcı olarak silinecek. Emin misiniz?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Sil", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
