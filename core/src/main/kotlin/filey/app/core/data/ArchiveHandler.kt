package filey.app.core.data

import com.github.junrar.Archive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.*
import org.apache.commons.compress.archivers.ArchiveEntry as LibArchiveEntry

object ArchiveHandler {

    data class ArchiveEntry(
        val name: String,
        val path: String,
        val size: Long,
        val isDirectory: Boolean,
        val compressedSize: Long = 0
    )

    suspend fun listContents(archivePath: String): List<ArchiveEntry> = withContext(Dispatchers.IO) {
        val ext = archivePath.substringAfterLast(".").lowercase()
        if (ext == "rar") listRar(archivePath) else listCommon(archivePath)
    }

    private fun listCommon(path: String): List<ArchiveEntry> {
        val entries = mutableListOf<ArchiveEntry>()
        try {
            val fis = BufferedInputStream(FileInputStream(path))
            val stream: ArchiveInputStream<out LibArchiveEntry> = ArchiveStreamFactory().createArchiveInputStream(fis)
            var e = stream.nextEntry
            while (e != null) {
                entries.add(ArchiveEntry(
                    name = e.name.substringAfterLast("/").ifEmpty { e.name },
                    path = e.name, size = e.size, isDirectory = e.isDirectory
                ))
                e = stream.nextEntry
            }
            stream.close()
        } catch (_: Exception) {}
        return entries
    }

    private fun listRar(path: String): List<ArchiveEntry> {
        val entries = mutableListOf<ArchiveEntry>()
        try {
            val archive = Archive(FileInputStream(path))
            for (h in archive.fileHeaders) {
                entries.add(ArchiveEntry(
                    name = h.fileName.substringAfterLast("\\").substringAfterLast("/"),
                    path = h.fileName, size = h.fullUnpackSize,
                    isDirectory = h.isDirectory, compressedSize = h.fullPackSize
                ))
            }
            archive.close()
        } catch (_: Exception) {}
        return entries
    }

    suspend fun extract(archivePath: String, destPath: String) = withContext(Dispatchers.IO) {
        val ext = archivePath.substringAfterLast(".").lowercase()
        val destDir = File(destPath).also { it.mkdirs() }
        if (ext == "rar") extractRar(archivePath, destDir) else extractCommon(archivePath, destDir)
    }

    private fun extractCommon(archivePath: String, destDir: File) {
        val fis = BufferedInputStream(FileInputStream(archivePath))
        val stream: ArchiveInputStream<out LibArchiveEntry> = ArchiveStreamFactory().createArchiveInputStream(fis)
        var e = stream.nextEntry
        val destDirPath = destDir.canonicalPath
        while (e != null) {
            val out = File(destDir, e.name)
            if (!out.canonicalPath.startsWith(destDirPath)) {
                throw SecurityException("Kötü niyetli zip girişi: ${e.name}")
            }
            if (e.isDirectory) { out.mkdirs() }
            else {
                out.parentFile?.mkdirs()
                FileOutputStream(out).use { stream.copyTo(it) }
            }
            e = stream.nextEntry
        }
        stream.close()
    }

    private fun extractRar(archivePath: String, destDir: File) {
        val archive = Archive(FileInputStream(archivePath))
        val destDirPath = destDir.canonicalPath
        for (h in archive.fileHeaders) {
            val out = File(destDir, h.fileName)
            if (!out.canonicalPath.startsWith(destDirPath)) {
                throw SecurityException("Kötü niyetli rar girişi: ${h.fileName}")
            }
            if (h.isDirectory) { out.mkdirs() }
            else {
                out.parentFile?.mkdirs()
                FileOutputStream(out).use { archive.extractFile(h, it) }
            }
        }
        archive.close()
    }

    suspend fun createZip(sourceFiles: List<String>, outputPath: String) = withContext(Dispatchers.IO) {
        val zos = ZipArchiveOutputStream(FileOutputStream(outputPath))
        for (fp in sourceFiles) {
            val f = File(fp)
            addToZip(zos, f, f.name)
        }
        zos.close()
    }

    private fun addToZip(zos: ZipArchiveOutputStream, file: File, entryName: String) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { addToZip(zos, it, "$entryName/${it.name}") }
        } else {
            zos.putArchiveEntry(ZipArchiveEntry(file, entryName))
            FileInputStream(file).use { it.copyTo(zos) }
            zos.closeArchiveEntry()
        }
    }
}
