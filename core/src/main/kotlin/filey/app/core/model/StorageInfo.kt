package filey.app.core.model

data class StorageInfo(
    val name: String,
    val path: String,
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val isInternal: Boolean
) {
    val usedRatio: Float get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
    val totalFormatted: String get() = formatSize(totalBytes)
    val usedFormatted: String get() = formatSize(usedBytes)
    val freeFormatted: String get() = formatSize(freeBytes)

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val g = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", bytes / Math.pow(1024.0, g.toDouble()), units[g])
    }
}
