package filey.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF1565C0),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFD1E4FF),
    secondary = androidx.compose.ui.graphics.Color(0xFF546E7A),
    tertiary = androidx.compose.ui.graphics.Color(0xFF00897B),
    error = androidx.compose.ui.graphics.Color(0xFFD32F2F)
)

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF90CAF9),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF0D47A1),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF1565C0),
    secondary = androidx.compose.ui.graphics.Color(0xFF90A4AE),
    tertiary = androidx.compose.ui.graphics.Color(0xFF4DB6AC),
    error = androidx.compose.ui.graphics.Color(0xFFEF5350)
)

@Composable
fun FileyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
