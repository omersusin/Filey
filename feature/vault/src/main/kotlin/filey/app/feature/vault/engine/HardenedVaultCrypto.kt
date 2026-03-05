package filey.app.feature.vault.engine

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class HardenedVaultCrypto(
    private val context: Context
) {
    
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "filey_vault_master_key"
        private const val KEK_ALIAS = "filey_vault_kek"  // Key Encryption Key
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128 // bits
        private const val GCM_IV_LENGTH = 12   // bytes (GCM standard)
        private const val PBKDF2_ITERATIONS = 310_000
        private const val SALT_LENGTH = 32
        private val MAGIC_BYTES = "FVLT".toByteArray(Charsets.US_ASCII)
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }
    
    // ═══════════════════════════════════════════
    //  TEE/StrongBox destekli master key üretimi
    // ═══════════════════════════════════════════
    
    fun generateMasterKey(
        requireBiometric: Boolean = true
    ): Boolean {
        if (keyStore.containsAlias(MASTER_KEY_ALIAS)) return true
        
        val purposes = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        
        val builder = KeyGenParameterSpec.Builder(MASTER_KEY_ALIAS, purposes)
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            // ╔════════════════════════════════════════╗
            // ║  KEY ASLA CİHAZ DIŞINA ÇIKARILMAZ     ║
            // ╚════════════════════════════════════════╝
            .setUnlockedDeviceRequired(true)   // Cihaz kilitliyken erişim yok
            .setRandomizedEncryptionRequired(true) // IV her seferinde rastgele
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(isStrongBoxAvailable())
        }
        
        if (requireBiometric) {
            builder
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true) 
                // yeni parmak izi eklenirse key invalidate olur!
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(
                    0, // her kullanımda auth gerekli (timeout=0)
                    KeyProperties.AUTH_BIOMETRIC_STRONG 
                        or KeyProperties.AUTH_DEVICE_CREDENTIAL
                )
            } else {
                // Legacy support for user auth duration
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(-1) // require auth for every use
            }
        }
        
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, 
            KEYSTORE_PROVIDER
        )
        keyGenerator.init(builder.build())
        keyGenerator.generateKey()
        
        return true
    }
    
    // StrongBox (ayrı bir güvenlik çipi) var mı?
    private fun isStrongBoxAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager
                .hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        } else false
    }
    
    // ═══════════════════════════════════════════
    //  Biometric + KeyStore entegre şifreleme
    // ═══════════════════════════════════════════
    
    fun getCipherForEncryption(): Cipher {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val key = keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
        cipher.init(Cipher.ENCRYPT_MODE, key)
        // GCM IV otomatik üretilir, cipher.iv ile alınır
        return cipher
    }
    
    fun getCipherForDecryption(iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val key = keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher
    }
    
    // ═══════════════════════════════════════════
    //  Dosya şifreleme (Streaming — büyük dosyalar)
    // ═══════════════════════════════════════════
    
    suspend fun encryptFile(
        source: InputStream,
        destination: OutputStream,
        cipher: Cipher  // BiometricPrompt'tan gelen authenticated cipher
    ) = withContext(Dispatchers.IO) {
        
        destination.use { out ->
            // Header yaz: [FORMAT_MAGIC][VERSION][IV_LENGTH][IV]
            out.write(MAGIC_BYTES)                    // 4 bytes: "FVLT"
            out.write(byteArrayOf(0x02))              // Version 2
            
            val iv = cipher.iv
            out.write(byteArrayOf(iv.size.toByte()))  // IV length
            out.write(iv)                             // IV
            
            // Encrypted data stream (GCM AuthTag otomatik eklenir)
            val cipherOut = CipherOutputStream(out, cipher)
            source.use { input ->
                input.copyTo(cipherOut)
            }
            cipherOut.flush()
            cipherOut.close()
        }
    }
    
    suspend fun decryptFile(
        source: InputStream,
        destination: OutputStream,
        cipher: Cipher
    ) = withContext(Dispatchers.IO) {
        
        source.use { input ->
            // Header oku ve doğrula
            val magic = ByteArray(4)
            input.read(magic)
            require(magic.contentEquals(MAGIC_BYTES)) { 
                "Invalid vault file format" 
            }
            
            val version = input.read()
            require(version == 2) { "Unsupported vault version: $version" }
            
            // IV length ve IV'yi header'dan oku
            val ivLength = input.read()
            val iv = ByteArray(ivLength)
            input.read(iv)
            
            // Decrypt stream
            val cipherIn = CipherInputStream(input, cipher)
            destination.use { out ->
                cipherIn.copyTo(out)
            }
            cipherIn.close()
        }
    }
    
    // ═══════════════════════════════════════════
    //  Parola tabanlı ek koruma katmanı (KEK)
    //  İsteğe bağlı: Biometric + Parola birlikte
    // ═══════════════════════════════════════════
    
    fun deriveKeyFromPassword(
        password: CharArray,
        salt: ByteArray = generateSalt()
    ): DerivedKeyResult {
        val spec = PBEKeySpec(
            password,
            salt,
            PBKDF2_ITERATIONS,
            256
        )
        
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        
        // Derived key'i KeyStore'a import et (donanım korumasına al)
        val keyEntry = KeyStore.SecretKeyEntry(
            SecretKeySpec(keyBytes, KeyProperties.KEY_ALGORITHM_AES)
        )
        
        val protection = KeyProtection.Builder(
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        
        keyStore.setEntry(KEK_ALIAS, keyEntry, protection)
        
        // Key bytes'ı RAM'den temizle
        keyBytes.fill(0)
        
        return DerivedKeyResult(salt = salt, success = true)
    }
    
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }
    
    // Vault'a erişimde anti-tamper kontrolü
    fun verifyIntegrity(): Boolean {
        return try {
            val key = keyStore.getKey(MASTER_KEY_ALIAS, null)
            key != null
        } catch (e: KeyPermanentlyInvalidatedException) {
            // Yeni biyometrik eklenmiş → key invalidate olmuş
            false
        } catch (e: UserNotAuthenticatedException) {
            // Auth gerekiyor — bu beklenen durum, key sağlam
            true
        } catch (e: Exception) {
            false
        }
    }
    
    data class DerivedKeyResult(
        val salt: ByteArray,
        val success: Boolean
    )
}
