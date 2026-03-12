package filey.app.core.root

class FakeShellRunner : ShellRunner {
    private val responses = mutableMapOf<String, ShellResult>()
    val executedCommands = mutableListOf<String>()

    fun givenResponse(commandContains: String, result: ShellResult) {
        responses[commandContains] = result
    }

    override suspend fun run(command: String): ShellResult {
        executedCommands.add(command)
        val key = responses.keys.firstOrNull { command.contains(it) }
        return responses[key] ?: ShellResult(127, emptyList(), listOf("command not found (fake)"))
    }
}
