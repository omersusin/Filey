package filey.app.feature.dashboard.domain

data class FileInsight(
    val type: InsightType,
    val title: String,
    val description: String,
    val suggestedAction: CommandBarAction?,
    val priority: Int
)

enum class InsightType {
    STORAGE_WARNING,     // "Depolama %90 dolu"
    DUPLICATE_FOUND,     // "47 kopya dosya bulundu (1.2GB)"
    CLEANUP_SUGGESTION,  // "3 aydır açılmayan 2.1GB dosya var"
    ORGANIZE_REMINDER,   // "İndirilenler'de 234 dosya tasnif edilmemiş"
    SECURITY_ALERT,      // "3 şifrelenmemiş hassas belge bulundu"
    BACKUP_REMINDER      // "Son yedekleme 14 gün önce"
}
