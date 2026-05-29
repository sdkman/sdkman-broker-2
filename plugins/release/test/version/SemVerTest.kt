package io.sdkman.kotlintoolchain.plugins.release.version

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SemVerTest {
    @Test
    fun parsesValidVersion() {
        assertEquals(SemVer(1, 2, 3), SemVer.parseOrNull("1.2.3"))
    }

    @Test
    fun trimsSurroundingWhitespace() {
        assertEquals(SemVer(0, 1, 67), SemVer.parseOrNull("  0.1.67  "))
    }

    @Test
    fun rejectsNonSemVerStrings() {
        assertNull(SemVer.parseOrNull("1.2"))
        assertNull(SemVer.parseOrNull("v1.2.3"))
        assertNull(SemVer.parseOrNull("1.2.3-rc1"))
        assertNull(SemVer.parseOrNull(""))
    }

    @Test
    fun incrementsPatchOnly() {
        assertEquals(SemVer(1, 2, 4), SemVer(1, 2, 3).incrementPatch())
    }
}
