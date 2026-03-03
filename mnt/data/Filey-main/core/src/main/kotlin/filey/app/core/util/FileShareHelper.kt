package filey.app.core.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * FileProvider kapsamını daraltabilmek için paylaşılacak dosyayı cache'e kopyalar.
 * Böylece FileProvider'da root-path / external-path "." gibi tehlikeli tanımlar gerekmez.
 */
object FileShareHelper {

    /**
     * Verilen dosyayı cache/shared altına kopyalar ve FileProvider URI döner.
     * Not: Büyük dosyalarda kopyalama maliyetlidir; mümkünse IO thread'inde çağır.
     */
    fun getShareableUri(context: Context, filePath: String): Uri {
        val source = File(filePath)
        val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val dest = File(sharedDir, source.name)
        source.copyTo(dest, overwrite = true)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            dest
        )
    }

    fun clearShareCache(context: Context) {
        File(context.cacheDir, "shared").deleteRecursively()
    }
}
