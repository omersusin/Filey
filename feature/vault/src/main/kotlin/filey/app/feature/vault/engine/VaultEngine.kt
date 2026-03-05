package filey.app.feature.vault.engine

import android.content.Context
import filey.app.feature.vault.engine.HardenedVaultCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.Cipher

class VaultEngine(
    private val context: Context,
    private val hardenedVaultCrypto: HardenedVaultCrypto = HardenedVaultCrypto(context)
) {

    suspend fun encryptFile(
        sourceFile: File,
        targetFile: File,
        cipher: Cipher
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            FileInputStream(sourceFile).use { fis ->
                FileOutputStream(targetFile).use { fos ->
                    hardenedVaultCrypto.encryptFile(fis, fos, cipher)
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
        cipher: Cipher
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            FileInputStream(sourceFile).use { fis ->
                FileOutputStream(targetFile).use { fos ->
                    hardenedVaultCrypto.decryptFile(fis, fos, cipher)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // Legacy support or helper to extract IV for decryption
    fun extractIv(file: File): ByteArray? {
        return try {
            FileInputStream(file).use { fis ->
                // Header: MAGIC(4) + VERSION(1) + IV_LENGTH(1)
                val header = ByteArray(6)
                fis.read(header)
                val ivLength = header[5].toInt() and 0xFF
                val iv = ByteArray(ivLength)
                fis.read(iv)
                iv
            }
        } catch (e: Exception) {
            null
        }
    }
}
