package filey.app.core.root

interface ShellRunner {
    suspend fun run(command: String): ShellResult
}

data class ShellResult(
    val exitCode: Int,
    val stdout: List<String>,
    val stderr: List<String>
)
