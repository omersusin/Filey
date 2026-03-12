package filey.app.feature.vault.engine

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import android.security.keystore.KeyPermanentlyInvalidatedException
import javax.crypto.Cipher

class BiometricVaultAuth(
    private val vaultCrypto: HardenedVaultCrypto
) {
    
    fun authenticateForEncryption(
        activity: FragmentActivity,
        onSuccess: (Cipher) -> Unit,
        onError: (String) -> Unit
    ) {
        val cipher = try {
            vaultCrypto.getCipherForEncryption()
        } catch (e: KeyPermanentlyInvalidatedException) {
            onError("Biyometrik veriler değişmiş. Vault'u yeniden kurmalısınız.")
            return
        } catch (e: Exception) {
            onError("Şifreleme başlatılamadı: ${e.message}")
            return
        }
        
        val cryptoObject = BiometricPrompt.CryptoObject(cipher)
        
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    val authenticatedCipher = result.cryptoObject?.cipher
                        ?: run {
                            onError("CryptoObject null!")
                            return
                        }
                    onSuccess(authenticatedCipher)
                }
                
                override fun onAuthenticationError(
                    errorCode: Int, errString: CharSequence
                ) {
                    onError(errString.toString())
                }
                
                override fun onAuthenticationFailed() {
                    // Handled by the system UI
                }
            }
        )
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Vault Erişimi")
            .setSubtitle("Şifreli dosyalarınıza erişmek için kimlik doğrulayın")
            .setNegativeButtonText("İptal")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setConfirmationRequired(true)
            .build()
        
        prompt.authenticate(promptInfo, cryptoObject)
    }
    
    fun authenticateForDecryption(
        activity: FragmentActivity,
        iv: ByteArray,
        onSuccess: (Cipher) -> Unit,
        onError: (String) -> Unit
    ) {
        val cipher = try {
            vaultCrypto.getCipherForDecryption(iv)
        } catch (e: Exception) {
            onError("Şifre çözme başlatılamadı: ${e.message}")
            return
        }
        
        val cryptoObject = BiometricPrompt.CryptoObject(cipher)
        
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    val authenticatedCipher = result.cryptoObject?.cipher
                        ?: run {
                            onError("CryptoObject null!")
                            return
                        }
                    onSuccess(authenticatedCipher)
                }
                
                override fun onAuthenticationError(
                    errorCode: Int, errString: CharSequence
                ) {
                    onError(errString.toString())
                }
                
                override fun onAuthenticationFailed() {
                }
            }
        )
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Vault Erişimi")
            .setSubtitle("Şifreli dosyalarınıza erişmek için kimlik doğrulayın")
            .setNegativeButtonText("İptal")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setConfirmationRequired(true)
            .build()
        
        prompt.authenticate(promptInfo, cryptoObject)
    }
}
