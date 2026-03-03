package com.omersusin.filey.core.util

import com.omersusin.filey.core.model.FileModel
import com.omersusin.filey.core.model.FileType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    fun fileToModel(file: File): FileModel {
        val ext = file.extension.lowercase()
        return FileModel(
            name = file.name,
            path = file.absolutePath,
            size = if (file.isDirectory) 0L else file.length(),
            lastModified = file.lastModified(),
            isDirectory = file.isDirectory,
            isHidden = file.isHidden,
            extension = ext,
            type = getFileType(ext, file.isDirectory),
            sizeFormatted = if (file.isDirectory) {
                val c = file.listFiles()?.size ?: 0
                "$c öğe"
            } else formatSize(file.length()),
            dateFormatted = formatDate(file.lastModified()),
            childCount = if (file.isDirectory) file.listFiles()?.size ?: 0 else 0
        )
    }

    fun getFileType(extension: String, isDirectory: Boolean): FileType {
        if (isDirectory) return FileType.DIRECTORY
        return when (extension) {
            in Constants.IMAGE_EXTENSIONS -> FileType.IMAGE
            in Constants.VIDEO_EXTENSIONS -> FileType.VIDEO
            in Constants.AUDIO_EXTENSIONS -> FileType.AUDIO
            in Constants.TEXT_EXTENSIONS -> FileType.TEXT
            in Constants.ARCHIVE_EXTENSIONS -> FileType.ARCHIVE
            in Constants.APK_EXTENSIONS -> FileType.APK
            in Constants.PDF_EXTENSIONS -> FileType.PDF
            else -> FileType.OTHER
        }
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val g = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", bytes / Math.pow(1024.0, g.toDouble()), units[g])
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getMimeType(path: String): String {
        val ext = path.substringAfterLast(".").lowercase()
        return when (ext) {
            in Constants.IMAGE_EXTENSIONS -> "image/*"
            in Constants.VIDEO_EXTENSIONS -> "video/*"
            in Constants.AUDIO_EXTENSIONS -> "audio/*"
            in Constants.TEXT_EXTENSIONS -> "text/*"
            in Constants.ARCHIVE_EXTENSIONS -> "application/zip"
            in Constants.APK_EXTENSIONS -> "application/vnd.android.package-archive"
            in Constants.PDF_EXTENSIONS -> "application/pdf"
            else -> "*/*"
        }
    }
}
