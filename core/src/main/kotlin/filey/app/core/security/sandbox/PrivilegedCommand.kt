package filey.app.core.security.sandbox

import java.io.File

/**
 * Ayrıcalıklı (Root/Shizuku) işlemler için tip-güvenli komut yapısı.
 */
sealed class PrivilegedCommand {
    abstract val requiresRoot: Boolean
    abstract val riskLevel: RiskLevel

    enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

    // --- Dosya Listeleme ---
    data class ListDirectory(
        val path: SafePath,
        val showHidden: Boolean = false
    ) : PrivilegedCommand() {
        override val requiresRoot = false
        override val riskLevel = RiskLevel.LOW
    }

    // --- Dosya Kopyalama ---
    data class CopyFile(
        val source: SafePath,
        val destination: SafePath
    ) : PrivilegedCommand() {
        override val requiresRoot = false
        override val riskLevel = RiskLevel.LOW
    }

    // --- Dosya Taşıma ---
    data class MoveFile(
        val source: SafePath,
        val destination: SafePath
    ) : PrivilegedCommand() {
        override val requiresRoot = false
        override val riskLevel = RiskLevel.MEDIUM
    }

    // --- Dosya Silme ---
    data class DeleteFile(
        val target: SafePath,
        val recursive: Boolean = false
    ) : PrivilegedCommand() {
        override val requiresRoot = false
        override val riskLevel = if (recursive) RiskLevel.HIGH else RiskLevel.MEDIUM
    }

    // --- İzin Değiştirme (chmod) ---
    data class ChangePermissions(
        val target: SafePath,
        val permissions: String // Örn: "755"
    ) : PrivilegedCommand() {
        override val requiresRoot = true
        override val riskLevel = RiskLevel.HIGH
    }

    // --- Sahiplik Değiştirme (chown) ---
    data class ChangeOwner(
        val target: SafePath,
        val owner: String,
        val group: String
    ) : PrivilegedCommand() {
        override val requiresRoot = true
        override val riskLevel = RiskLevel.CRITICAL
    }

    /**
     * Komutu güvenli bir shell komut dizisine çevirir.
     */
    fun compile(): String {
        return when (this) {
            is ListDirectory -> {
                val flags = if (showHidden) "-la" else "-l"
                "ls $flags ${path.shellEscaped()}"
            }
            is CopyFile -> "cp -f ${source.shellEscaped()} ${destination.shellEscaped()}"
            is MoveFile -> "mv -f ${source.shellEscaped()} ${destination.shellEscaped()}"
            is DeleteFile -> {
                val flags = if (recursive) "-rf" else "-f"
                "rm $flags ${target.shellEscaped()}"
            }
            is ChangePermissions -> "chmod $permissions ${target.shellEscaped()}"
            is ChangeOwner -> "chown $owner:$group ${target.shellEscaped()}"
        }
    }
}
