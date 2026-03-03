package filey.app.feature.browser.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PermissionRequiredContent(
    onRequestPermission: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.FolderOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text("Dosya erişimi gerekli", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Filey'in dosyalarınıza erişebilmesi için izin vermeniz gerekiyor.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRequestPermission) {
                Text("İzin Ver")
            }
        }
    }
}

@Composable
fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry) {
                Text("Tekrar Dene")
            }
        }
    }
}

@Composable
fun EmptyContent(isSearchActive: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                if (isSearchActive) Icons.Outlined.SearchOff else Icons.Outlined.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (isSearchActive) "Sonuç bulunamadı" else "Bu klasör boş",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
