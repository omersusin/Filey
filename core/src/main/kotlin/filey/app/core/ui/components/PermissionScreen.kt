package filey.app.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    PermissionRationaleScreen(
        onRequestPermission = onRequestPermission,
        modifier = modifier
    )
}

@Composable
fun PermissionRationaleScreen(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    PermissionBase(
        icon = Icons.Default.Storage,
        title = "Dosyalara erişim izni gerekli",
        description = "Filey dosyalarınızı yönetebilmesi için depolama iznine ihtiyacı var.",
        actionText = "İzin Ver",
        onAction = onRequestPermission,
        modifier = modifier
    )
}

@Composable
fun ManageFilesPermissionScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    PermissionBase(
        icon = Icons.Default.Settings,
        title = "Tüm dosyalara erişim gerekli",
        description = "Android 11+ cihazlarda Filey'in çalışması için “Tüm dosyalara erişim” izni vermen gerekiyor.",
        actionText = "Ayarları Aç",
        onAction = onOpenSettings,
        modifier = modifier
    )
}

@Composable
private fun PermissionBase(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAction) {
                Text(text = actionText)
            }
        }
    }
}
