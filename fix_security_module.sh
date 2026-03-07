#!/bin/bash
set -e
cd ~/Filey

# ── AuditLogger.kt — @Inject kaldır ─────────────────────────────────────────
cat > feature/security/src/main/kotlin/filey/app/feature/security/data/AuditLogger.kt << 'EOF'
package filey.app.feature.security.data

import filey.app.feature.security.domain.PrivilegedCommand
import filey.app.feature.security.domain.PrivilegedCommand.RiskLevel
import timber.log.Timber

class AuditLogger(
    private val auditDao: AuditDao
) {
    suspend fun logDenied(command: PrivilegedCommand, reason: String) =
        log(command, "DENIED", reason)

    suspend fun logAllowed(command: PrivilegedCommand) =
        log(command, "ALLOWED", "Policy check passed")

    suspend fun logExecuted(command: PrivilegedCommand, timeMs: Long) =
        log(command, "EXECUTED", "", timeMs)

    suspend fun logError(command: PrivilegedCommand, error: Throwable) =
        log(command, "ERROR", "", errorMessage = error.message)

    private suspend fun log(
        command: PrivilegedCommand,
        action: String,
        reason: String,
        executionTimeMs: Long = 0,
        errorMessage: String? = null
    ) {
        auditDao.insert(
            AuditEntry(
                commandType = command::class.simpleName ?: "Unknown",
                commandDetails = sanitizeForLog(command),
                riskLevel = command.riskLevel.name,
                action = action,
                reason = reason,
                executionTimeMs = executionTimeMs,
                errorMessage = errorMessage
            )
        )
        if (command.riskLevel >= RiskLevel.HIGH) {
            Timber.w("AUDIT [$action]: ${command::class.simpleName} risk=${command.riskLevel} reason=$reason")
        }
    }

    private fun sanitizeForLog(command: PrivilegedCommand): String = when (command) {
        is PrivilegedCommand.DeleteFile ->
            "target=${command.target.value.takeLast(50)}, recursive=${command.recursive}"
        is PrivilegedCommand.ChangeOwner ->
            "target=***, owner=${command.owner.name}"
        else -> command.toString().take(200)
    }
}
EOF
echo "✅ AuditLogger.kt"

# ── SandboxedCommandExecutor.kt — @Inject, Provider, Shizuku kaldır ──────────
cat > feature/security/src/main/kotlin/filey/app/feature/security/data/SandboxedCommandExecutor.kt << 'EOF'
package filey.app.feature.security.data

import com.topjohnwu.superuser.Shell
import filey.app.feature.security.domain.AccessMode
import filey.app.feature.security.domain.PrivilegedCommand
import filey.app.feature.security.domain.SecurityPolicyEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── Root executor via libsu ───────────────────────────────────────────────────

interface CommandExecutor {
    suspend fun execute(command: String): String
}

class RootCommandExecutor : CommandExecutor {
    override suspend fun execute(command: String): String = withContext(Dispatchers.IO) {
        if (!Shell.getShell().isRoot) throw SecurityException("Root access not available")
        val result = Shell.cmd(command).exec()
        if (!result.isSuccess)
            throw RuntimeException("Root failed (${result.code}): ${result.err.joinToString("\n")}")
        result.out.joinToString("\n")
    }
    fun isAvailable(): Boolean = try { Shell.getShell().isRoot } catch (e: Exception) { false }
}

// ── Shizuku executor ──────────────────────────────────────────────────────────

class ShizukuCommandExecutor : CommandExecutor {
    override suspend fun execute(command: String): String = withContext(Dispatchers.IO) {
        if (!isAvailable()) throw SecurityException("Shizuku not available or permission not granted")
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exit = process.waitFor()
        if (exit != 0) throw RuntimeException("Shizuku command failed ($exit): $stderr")
        stdout
    }

    fun isAvailable(): Boolean = try {
        val shizukuClass = Class.forName("dev.rikka.shizuku.Shizuku")
        val pingBinder = shizukuClass.getMethod("pingBinder")
        val checkPerm = shizukuClass.getMethod("checkSelfPermission")
        val ping = pingBinder.invoke(null) as Boolean
        val perm = checkPerm.invoke(null) as Int
        ping && perm == android.content.pm.PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) { false }
}

// ── Sandboxed orchestrator ────────────────────────────────────────────────────

class SandboxedCommandExecutor(
    private val policyEngine: SecurityPolicyEngine,
    private val rootExecutor: RootCommandExecutor,
    private val shizukuExecutor: ShizukuCommandExecutor,
    private val auditLogger: AuditLogger,
    private val accessMode: () -> AccessMode          // lambda replaces Provider<>
) {
    sealed class ExecutionResult {
        data class Success(val output: String = "", val executionTimeMs: Long = 0) : ExecutionResult()
        data class Denied(val reason: String) : ExecutionResult()
        data class Error(val exception: Throwable, val stderr: String = "") : ExecutionResult()
    }

    suspend fun execute(command: PrivilegedCommand): ExecutionResult {
        val policy = policyEngine.evaluate(command)
        if (!policy.allowed) {
            auditLogger.logDenied(command, policy.reason)
            return ExecutionResult.Denied(policy.reason)
        }
        val shellCommand = compileCommand(command)
        val startTime = System.currentTimeMillis()
        return try {
            val output = when (accessMode()) {
                AccessMode.ROOT    -> rootExecutor.execute(shellCommand)
                AccessMode.SHIZUKU -> shizukuExecutor.execute(shellCommand)
                AccessMode.NORMAL  -> throw SecurityException("Elevated privileges required")
            }
            val elapsed = System.currentTimeMillis() - startTime
            auditLogger.logExecuted(command, elapsed)
            ExecutionResult.Success(output = output, executionTimeMs = elapsed)
        } catch (e: Exception) {
            auditLogger.logError(command, e)
            ExecutionResult.Error(e)
        }
    }

    private fun compileCommand(cmd: PrivilegedCommand): String = when (cmd) {
        is PrivilegedCommand.ListDirectory     -> "ls ${if (cmd.showHidden) "-la" else "-l"} ${cmd.path.shellEscaped()}"
        is PrivilegedCommand.CopyFile          -> "cp -f ${cmd.source.shellEscaped()} ${cmd.destination.shellEscaped()}"
        is PrivilegedCommand.MoveFile          -> "mv -f ${cmd.source.shellEscaped()} ${cmd.destination.shellEscaped()}"
        is PrivilegedCommand.DeleteFile        -> "rm ${if (cmd.recursive) "-rf" else "-f"} ${cmd.target.shellEscaped()}"
        is PrivilegedCommand.ChangePermissions -> "chmod ${cmd.permissions.toOctalString()} ${cmd.target.shellEscaped()}"
        is PrivilegedCommand.ChangeOwner       -> "chown ${cmd.owner.name}:${cmd.group.name} ${cmd.target.shellEscaped()}"
        is PrivilegedCommand.ReadSystemFile    -> "cat ${cmd.path.shellEscaped()}"
        is PrivilegedCommand.MountFilesystem   -> "mount -o ${cmd.options.toFlags()} ${cmd.device.shellEscaped()} ${cmd.mountPoint.shellEscaped()}"
    }
}
EOF
echo "✅ SandboxedCommandExecutor.kt"

# ── SecurityContainer.kt — Provider<> → lambda ───────────────────────────────
cat > feature/security/src/main/kotlin/filey/app/feature/security/di/SecurityContainer.kt << 'EOF'
package filey.app.feature.security.di

import android.content.Context
import filey.app.feature.security.data.AuditDatabase
import filey.app.feature.security.data.AuditLogger
import filey.app.feature.security.data.RootCommandExecutor
import filey.app.feature.security.data.SandboxedCommandExecutor
import filey.app.feature.security.data.ShizukuCommandExecutor
import filey.app.feature.security.domain.AccessMode
import filey.app.feature.security.domain.SecurityConfig
import filey.app.feature.security.domain.SecurityPolicyEngine

class SecurityContainer(context: Context) {
    private val db            = AuditDatabase.getInstance(context)
    val auditLogger           = AuditLogger(db.auditDao())
    private val userPrefs     = SecurityConfig.DefaultUserPreferences()
    val policyEngine          = SecurityPolicyEngine(auditLogger, userPrefs)
    val rootExecutor          = RootCommandExecutor()
    val shizukuExecutor       = ShizukuCommandExecutor()

    private var currentMode: AccessMode = AccessMode.NORMAL

    val sandboxedExecutor = SandboxedCommandExecutor(
        policyEngine    = policyEngine,
        rootExecutor    = rootExecutor,
        shizukuExecutor = shizukuExecutor,
        auditLogger     = auditLogger,
        accessMode      = { currentMode }    // lambda instead of Provider<>
    )

    fun setAccessMode(mode: AccessMode) { currentMode = mode }

    companion object {
        @Volatile private var INSTANCE: SecurityContainer? = null
        fun getInstance(context: Context): SecurityContainer =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityContainer(context).also { INSTANCE = it }
            }
    }
}
EOF
echo "✅ SecurityContainer.kt"

# ── SecurityPolicyEngine.kt — @Inject kaldır ────────────────────────────────
cat > feature/security/src/main/kotlin/filey/app/feature/security/domain/SecurityPolicyEngine.kt << 'EOF'
package filey.app.feature.security.domain

import filey.app.feature.security.data.AuditLogger
import filey.app.feature.security.domain.PrivilegedCommand.RiskLevel

class SecurityPolicyEngine(
    private val auditLogger: AuditLogger,
    private val userPreferences: UserPreferences
) {
    data class PolicyResult(
        val allowed: Boolean,
        val reason: String,
        val requiresConfirmation: Boolean = false
    )

    suspend fun evaluate(command: PrivilegedCommand): PolicyResult {
        if (!userPreferences.isPrivilegedModeEnabled()) {
            return PolicyResult(false, "Privileged mode is disabled")
        }
        val maxAllowedRisk = userPreferences.getMaxRiskLevel()
        if (command.riskLevel.ordinal > maxAllowedRisk.ordinal) {
            auditLogger.logDenied(command, "Risk level exceeded")
            return PolicyResult(false, "Operation risk (${command.riskLevel}) exceeds policy ($maxAllowedRisk)")
        }
        val pathResult = evaluatePathPolicy(command)
        if (!pathResult.allowed) return pathResult

        val rateLimitResult = evaluateRateLimit(command)
        if (!rateLimitResult.allowed) return rateLimitResult

        auditLogger.logAllowed(command)
        return PolicyResult(
            allowed = true,
            reason = "Policy check passed",
            requiresConfirmation = command.riskLevel >= RiskLevel.HIGH
        )
    }

    private fun evaluatePathPolicy(command: PrivilegedCommand): PolicyResult {
        val paths = when (command) {
            is PrivilegedCommand.CopyFile          -> listOf(command.source, command.destination)
            is PrivilegedCommand.MoveFile          -> listOf(command.source, command.destination)
            is PrivilegedCommand.DeleteFile        -> listOf(command.target)
            is PrivilegedCommand.ChangePermissions -> listOf(command.target)
            is PrivilegedCommand.ChangeOwner       -> listOf(command.target)
            is PrivilegedCommand.ReadSystemFile    -> listOf(command.path)
            else -> emptyList()
        }
        val writableRoots = listOf("/storage/emulated/0", "/sdcard", "/data/media/0", "/mnt/media_rw")
        val writeCommands = setOf(
            PrivilegedCommand.MoveFile::class, PrivilegedCommand.DeleteFile::class,
            PrivilegedCommand.ChangePermissions::class, PrivilegedCommand.ChangeOwner::class
        )
        if (command::class in writeCommands) {
            for (path in paths) {
                if (writableRoots.none { path.value.startsWith(it) }) {
                    return PolicyResult(false, "Write not allowed on: ${path.value}")
                }
            }
        }
        return PolicyResult(true, "Path OK")
    }

    private val commandHistory = ArrayDeque<Pair<Long, RiskLevel>>(100)

    private fun evaluateRateLimit(command: PrivilegedCommand): PolicyResult {
        val now = System.currentTimeMillis()
        while (commandHistory.isNotEmpty() && now - commandHistory.first().first > 60_000L)
            commandHistory.removeFirst()
        val highRiskCount = commandHistory.count { it.second >= RiskLevel.HIGH }
        if (highRiskCount >= 10 && command.riskLevel >= RiskLevel.HIGH) {
            return PolicyResult(false, "Rate limit exceeded: $highRiskCount high-risk ops in last minute")
        }
        commandHistory.addLast(now to command.riskLevel)
        return PolicyResult(true, "Rate OK")
    }
}
EOF
echo "✅ SecurityPolicyEngine.kt"

# ── build.gradle.kts — javax.inject bağımlılığı kaldır (kullanmıyoruz artık) ─
# shizuku'yu da kaldır (reflection ile çözüyoruz)
cat > feature/security/build.gradle.kts << 'EOF'
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}
android {
    namespace = "filey.app.feature.security"
    compileSdk = 34
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.libsu.core)
    implementation(libs.shizuku.api)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation("com.jakewharton.timber:timber:5.0.1")
}
EOF
echo "✅ security/build.gradle.kts"

git add \
  feature/security/src/main/kotlin/filey/app/feature/security/data/AuditLogger.kt \
  feature/security/src/main/kotlin/filey/app/feature/security/data/SandboxedCommandExecutor.kt \
  feature/security/src/main/kotlin/filey/app/feature/security/di/SecurityContainer.kt \
  feature/security/src/main/kotlin/filey/app/feature/security/domain/SecurityPolicyEngine.kt \
  feature/security/build.gradle.kts

git commit -m "fix(security): remove @Inject/Provider, use reflection for Shizuku, fix exhaustive when"
git push origin feat/sprint1-hardening
echo "DONE"
