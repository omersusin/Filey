package filey.app.core.root

import org.junit.Assert.assertEquals
import org.junit.Test

class ShellEscapeTest {
    private val rm = RootManager.createForTest(FakeShellRunner())

    @Test fun `normal path stays wrapped`() =
        assertEquals("'/sdcard/file.txt'", rm.shellEscape("/sdcard/file.txt"))

    @Test fun `space in path is handled`() =
        assertEquals("'/sdcard/My Files/a.txt'", rm.shellEscape("/sdcard/My Files/a.txt"))

    @Test fun `single quote is escaped`() =
        assertEquals("'/sdcard/it'\\''s.txt'", rm.shellEscape("/sdcard/it's.txt"))

    @Test fun `injection attempt is neutralised`() {
        val result = rm.shellEscape("/sdcard/file; rm -rf /")
        assertEquals("'/sdcard/file; rm -rf /'", result)
    }

    @Test fun `empty string gives empty quotes`() =
        assertEquals("''", rm.shellEscape(""))
}
