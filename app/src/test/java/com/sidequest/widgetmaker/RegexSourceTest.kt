package com.sidequest.widgetmaker

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Android's regex engine (ICU) rejects a "}" that isn't escaped or closing a {n,m} count, though the
 * JVM's (which these tests run on) accepts it: such a pattern passes here and crashes on the phone.
 * So every raw-string Regex in the app is checked for one.
 */
class RegexSourceTest {
    @Test
    fun noBareClosingBraceInPatterns() {
        val bad = mutableListOf<String>()
        File("src/main/java").walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            Regex("Regex\\(\"\"\"(.*?)\"\"\"").findAll(file.readText()).forEach { m ->
                val pattern = m.groupValues[1]
                // Drop escaped braces and {n}, {n,} or {n,m} counts; any "}" left over is bare.
                val rest = pattern.replace("\\\\[{}]".toRegex(), "").replace("\\{\\d+(,\\d*)?\\}".toRegex(), "")
                if ('}' in rest) bad += "${file.name}: $pattern"
            }
        }
        assertTrue("Bare } in: $bad", bad.isEmpty())
    }
}
