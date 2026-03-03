package filey.app.core.root

import filey.app.core.result.FileResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RootManagerTest {

    private lateinit var fake: FakeShellRunner
    private lateinit var rm: RootManager

    @Before fun setup() {
        fake = FakeShellRunner()
        rm   = RootManager.createForTest(fake)
    }

    @Test fun `ls success returns Success`() = runTest {
        fake.givenResponse("ls", ShellResult(0, listOf("a.txt", "b.jpg"), emptyList()))
        val result = rm.execute("ls", listOf("-la"), listOf("/sdcard"))
        assertTrue(result is FileResult.Success)
        assertEquals(listOf("a.txt", "b.jpg"), (result as FileResult.Success).data)
    }

    @Test fun `disallowed command returns Error`() = runTest {
        val result = rm.execute("reboot", emptyList(), emptyList())
        assertTrue(result is FileResult.Error)
    }

    @Test fun `protected path returns PermissionDenied`() = runTest {
        val result = rm.execute("rm", listOf("-rf"), listOf("/system"))
        assertTrue(result is FileResult.Error.PermissionDenied)
    }

    @Test fun `shell failure returns ShellCommandFailed`() = runTest {
        fake.givenResponse("ls", ShellResult(2, emptyList(), listOf("No such file")))
        val result = rm.execute("ls", emptyList(), listOf("/nonexistent"))
        assertTrue(result is FileResult.Error.ShellCommandFailed)
        assertEquals(2, (result as FileResult.Error.ShellCommandFailed).exitCode)
    }

    @Test fun `invalid flag returns Error`() = runTest {
        val result = rm.execute("rm", listOf("--no-preserve-root"), listOf("/sdcard/file.txt"))
        assertTrue(result is FileResult.Error)
    }
}
