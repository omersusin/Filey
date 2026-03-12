package filey.app.core.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealShellRunner : ShellRunner {
    override suspend fun run(command: String): ShellResult = withContext(Dispatchers.IO) {
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
        val stdout = process.inputStream.bufferedReader().readLines()
        val stderr = process.errorStream.bufferedReader().readLines()
        val exitCode = process.waitFor()
        ShellResult(exitCode, stdout, stderr)
    }
}
