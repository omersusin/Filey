package filey.app.feature.vault.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onBack: () -> Unit
) {
    var isUnlocked by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Biometric Auth
    val authenticate = {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            context as FragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isUnlocked = true
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Kasa Kilidini Aç")
            .setSubtitle("Biyometrik verinizi kullanarak giriş yapın")
            .setNegativeButtonText("PIN Kullan")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // Auto-start biometric if available
    LaunchedEffect(Unit) {
        val biometricManager = BiometricManager.from(context)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            authenticate()
        }
    }

    if (!isUnlocked) {
        VaultAuthScreen(
            pin = pin,
            error = error,
            onPinChange = { 
                pin = it
                if (pin.length == 4) {
                    if (pin == "1234") { // Mock PIN
                        isUnlocked = true
                    } else {
                        error = true
                        pin = ""
                    }
                }
            },
            onBiometricClick = { authenticate() },
            onBack = onBack
        )
    } else {
        VaultContentScreen(onBack = onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultAuthScreen(
    pin: String,
    error: Boolean,
    onPinChange: (String) -> Unit,
    onBiometricClick: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Güvenli Kasa",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Erişmek için PIN girin",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(48.dp))

        // PIN Indicators
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { index ->
                val filled = index < pin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        if (error) {
            Spacer(Modifier.height(16.dp))
            Text("Yanlış PIN, tekrar deneyin", color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(48.dp))

        // Simple Keypad
        val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "BIO", "0", "C")
        LazyColumn(modifier = Modifier.width(280.dp)) {
            items(numbers.chunked(3)) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { num ->
                        FilledTonalIconButton(
                            onClick = {
                                when (num) {
                                    "C" -> if (pin.isNotEmpty()) onPinChange(pin.dropLast(1))
                                    "BIO" -> onBiometricClick()
                                    else -> if (pin.length < 4) onPinChange(pin + num)
                                }
                            },
                            modifier = Modifier.size(64.dp),
                            enabled = num.isNotEmpty()
                        ) {
                            when (num) {
                                "C" -> Icon(Icons.Default.Backspace, null)
                                "BIO" -> Icon(Icons.Default.Fingerprint, null)
                                else -> Text(num, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onBack) { Text("İptal") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultContentScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kasa İçeriği") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Add file to vault */ }) {
                        Icon(Icons.Default.Add, "Ekle")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Shield,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.outlineVariant,
                contentDescription = null
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Kasa Boş",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Dosyalarınızı buraya taşıyarak şifreleyebilirsiniz",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
