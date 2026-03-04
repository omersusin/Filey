package filey.app.feature.vault.engine

import java.io.*
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VaultEngine {

    companion object {
        private const val ALGORITHM = "AES/CBC/PKCS5Padding"
        private const val KEY_SIZE = 32 // 256 bit
        private const val IV_SIZE = 16 // 128 bit
    }

    suspend fun encryptFile(
        sourceFile: File,
        targetFile: File,
        passwordHash: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = generateKey(passwordHash)
            val iv = generateIv()
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))

            FileOutputStream(targetFile).use { fos ->
                // Write IV first
                fos.write(iv)
                CipherOutputStream(fos, cipher).use { cos ->
                    FileInputStream(sourceFile).use { fis ->
                        fis.copyTo(cos)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun decryptFile(
        sourceFile: File,
        targetFile: File,
        passwordHash: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            FileInputStream(sourceFile).use { fis ->
                // Read IV first
                val iv = ByteArray(IV_SIZE)
                fis.read(iv)

                val key = generateKey(passwordHash)
                val cipher = Cipher.getInstance(ALGORITHM)
                cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

                CipherInputStream(fis, cipher).use { cis ->
                    FileOutputStream(targetFile).use { fos ->
                        cis.copyTo(fos)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun generateKey(password: String): SecretKeySpec {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(password.toByteArray())
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun generateIv(): ByteArray {
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        return iv
    }
}
