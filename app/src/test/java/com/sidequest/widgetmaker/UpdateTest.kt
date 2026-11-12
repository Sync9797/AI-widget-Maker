package com.sidequest.widgetmaker

import com.sidequest.widgetmaker.update.Release
import com.sidequest.widgetmaker.update.UpdateMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateTest {
    private val body = """{"tag_name":"latest","target_commitish":"abc123","name":"AI Widget Maker 0.9.3 (abc123)",
        "assets":[{"name":"AIWidgetMaker.apk","browser_download_url":"https://github.com/x/y/releases/download/latest/AIWidgetMaker.apk"}]}"""

    @Test
    fun readsGitHubsLatestRelease() {
        val r = UpdateMath.parse(body)!!
        assertEquals("abc123", r.sha)
        assertEquals("0.9.3", r.version)
        assertTrue(r.apkUrl.endsWith("AIWidgetMaker.apk"))
        assertNull(UpdateMath.parse("""{"name":"x","assets":[]}"""))
    }

    @Test
    fun aDifferentBuildIsNewer() {
        val r = Release("abc123", "0.9.3", "u")
        assertTrue(UpdateMath.isNewer(r, "def456", "0.9.3"))
        assertFalse(UpdateMath.isNewer(r, "abc123", "0.9.3"))
        // A local build doesn't know its commit: versions decide.
        assertTrue(UpdateMath.isNewer(r, "dev", "0.9.2"))
        assertFalse(UpdateMath.isNewer(r, "dev", "0.10.0"))
        // An older release title without a version: no update for a local build.
        assertFalse(UpdateMath.isNewer(Release("abc", null, "u"), "dev", "0.9.2"))
    }

    @Test
    fun versionsCompareByNumber() {
        assertTrue(UpdateMath.compare("0.10.0", "0.9.9") > 0)
        assertEquals(0, UpdateMath.compare("1.0", "1.0.0"))
    }
}
