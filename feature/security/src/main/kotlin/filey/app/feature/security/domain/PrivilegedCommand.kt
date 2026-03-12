package filey.app.feature.security.domain

import java.io.File

sealed class PrivilegedCommand {
    abstract val requiresRoot: Boolean
    abstract val riskLevel: RiskLevel

    enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

    data class ListDirectory(
        val path: SafePath,
        val showHidden: Boolean = false
    ) : PrivilegedCommand() {
        override val requiresRoot = false
        override val riskLevel = RiskLevel.LOW
    }

    data class CopyFile(
        val source: SafePath,
        val destination: SafePath,
        val preservePermissions: Boolean = true
    ) : PrivilegedCommand() {
        override val requiresRoot = false
        override val riskLevel = RiskLevel.LOW
    }

    data class MoveFile(
        val source: SafePath,
        val destination: SafePath
    ) : PrivilegedCommand() {
        override val requiresRoot = false
        override val riskLevel = RiskLevel.MEDIUM
    }

    data class DeleteFile(
        val target: SafePath,
        val recursive: Boolean = false
    ) : PrivilegedCommand() {
        override val requiresRoot = false
        override val riskLevel = if (recursive) RiskLevel.HIGH else RiskLevel.MEDIUM
    }

    data class ChangePermissions(
        val target: SafePath,
        val permissions: FilePermissions
    ) : PrivilegedCommand() {
        override val requiresRoot = true
        override val riskLevel = RiskLevel.HIGH
    }

    data class ChangeOwner(
        val target: SafePath,
        val owner: UnixUser,
        val group: UnixGroup
    ) : PrivilegedCommand() {
        override val requiresRoot = true
        override val riskLevel = RiskLevel.CRITICAL
    }

    data class MountFilesystem(
        val device: SafePath,
        val mountPoint: SafePath,
        val options: MountOptions
    ) : PrivilegedCommand() {
        override val requiresRoot = true
        override val riskLevel = RiskLevel.CRITICAL
    }

    data class ReadSystemFile(
        val path: SafePath
    ) : PrivilegedCommand() {
        override val requiresRoot = true
        override val riskLevel = RiskLevel.HIGH
    }
}

@JvmInline
value class SafePath private constructor(val value: String) {
    companion object {
        fun of(rawPath: String): SafePath? {
            val normalized = rawPath.replace("\\", "/")
                .let { File(it).canonicalPath }

            if (normalized.contains("../") || normalized.contains("/./")) {
                return null
            }

            val forbiddenPrefixes = listOf(
                "/system/bin", "/system/xbin", "/system/etc",
                "/sbin", "/proc", "/sys/kernel",
                "/data/data",
                "/data/system"
            )

            if (forbiddenPrefixes.any { normalized.startsWith(it) }) {
                return null
            }

            return SafePath(normalized)
        }

        internal fun unsafe(path: String) = SafePath(path)
    }

    fun shellEscaped(): String {
        return "'${value.replace("'", "'\\''")}'"
    }
}

data class FilePermissions(
    val owner: PermissionSet,
    val group: PermissionSet,
    val others: PermissionSet
) {
    data class PermissionSet(
        val read: Boolean,
        val write: Boolean,
        val execute: Boolean
    ) {
        fun toOctal(): Int {
            var octal = 0
            if (read) octal += 4
            if (write) octal += 2
            if (execute) octal += 1
            return octal
        }
    }

    fun toOctalString(): String {
        return "${owner.toOctal()}${group.toOctal()}${others.toOctal()}"
    }
}

data class UnixUser(val name: String)
data class UnixGroup(val name: String)
data class MountOptions(val flags: List<String>) {
    fun toFlags(): String = flags.joinToString(",")
}
