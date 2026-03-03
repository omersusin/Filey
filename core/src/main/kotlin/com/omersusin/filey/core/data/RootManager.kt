package com.omersusin.filey.core.data

import com.topjohnwu.superuser.Shell

object RootManager {

    val isRootAvailable: Boolean
        get() = Shell.isAppGrantedRoot() == true

    fun init() {
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(10)
        )
    }

    fun execute(command: String): Shell.Result = Shell.cmd(command).exec()

    fun listAsRoot(path: String): List<String> {
        val r = Shell.cmd("ls -la '$path'").exec()
        return if (r.isSuccess) r.out else emptyList()
    }

    fun readAsRoot(path: String): String? {
        val r = Shell.cmd("cat '$path'").exec()
        return if (r.isSuccess) r.out.joinToString("\n") else null
    }

    fun deleteAsRoot(path: String): Boolean =
        Shell.cmd("rm -rf '$path'").exec().isSuccess

    fun copyAsRoot(src: String, dst: String): Boolean =
        Shell.cmd("cp -rf '$src' '$dst'").exec().isSuccess

    fun moveAsRoot(src: String, dst: String): Boolean =
        Shell.cmd("mv -f '$src' '$dst'").exec().isSuccess
}
