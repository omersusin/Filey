package filey.app

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import filey.app.navigation.NavGraph
import filey.app.ui.theme.FileyTheme
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val shizukuPermissionCode = 100

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == shizukuPermissionCode) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Shizuku izni verildi", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Shizuku izni reddedildi", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Shizuku listener
        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        } catch (_: Exception) {
            // Shizuku not available, ignore
        }

        // Android 11+ file access
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    startActivity(
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                } catch (_: Exception) {
                    startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }
        }

        setContent {
            FileyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        } catch (_: Exception) { }
    }
}
