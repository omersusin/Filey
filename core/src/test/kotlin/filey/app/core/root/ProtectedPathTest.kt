package filey.app.core.root

import org.junit.Assert.*
import org.junit.Test

class ProtectedPathTest {
    private val rm = RootManager.createForTest(FakeShellRunner())

    @Test fun `root is protected`()              = assertTrue(rm.isProtectedPath("/"))
    @Test fun `system is protected`()            = assertTrue(rm.isProtectedPath("/system"))
    @Test fun `system sub-path is protected`()   = assertTrue(rm.isProtectedPath("/system/app"))
    @Test fun `data is protected`()              = assertTrue(rm.isProtectedPath("/data"))
    @Test fun `apex is protected`()              = assertTrue(rm.isProtectedPath("/apex"))
    @Test fun `systemui is NOT protected`()      = assertFalse(rm.isProtectedPath("/systemui"))
    @Test fun `sdcard is NOT protected`()        = assertFalse(rm.isProtectedPath("/sdcard/Documents"))
    @Test fun `storage emulated NOT protected`() =
        assertFalse(rm.isProtectedPath("/storage/emulated/0/Download"))
}
