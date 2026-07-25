package com.javaacademy.plugin

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Threading audit: verifies that blocking HTTP calls (httpClient.newCall().execute()) are only
 * in classes that are always invoked from a pooled thread, never directly from actionPerformed
 * or init blocks that run on the EDT.
 */
class ThreadingAuditTest {

    private val sourceRoot = System.getProperty("user.dir") + "/src/main/kotlin"

    private fun readSources(relativePath: String): String {
        val file = java.io.File(sourceRoot, relativePath)
        return if (file.exists()) file.readText() else ""
    }

    @Test
    fun `StartTaskAction_actionPerformed_delegatesToPooledThread`() {
        val source = readSources("com/javaacademy/plugin/actions/StartTaskAction.kt")
        assertFalse(source.isEmpty(), "StartTaskAction.kt must exist")
        assertTrue(source.contains("executeOnPooledThread"), "actionPerformed must delegate network I/O to a pooled thread")
        assertFalse(
            containsDirectNetworkCallInActionPerformed(source),
            "actionPerformed must not call httpClient.newCall directly (only via pooled thread)"
        )
    }

    @Test
    fun `VerifyTaskAction_actionPerformed_delegatesToPooledThread`() {
        val source = readSources("com/javaacademy/plugin/actions/VerifyTaskAction.kt")
        assertFalse(source.isEmpty(), "VerifyTaskAction.kt must exist")
        assertTrue(source.contains("executeOnPooledThread"), "actionPerformed must delegate network I/O to a pooled thread")
    }

    @Test
    fun `GetHintAction_actionPerformed_delegatesToPooledThread`() {
        val source = readSources("com/javaacademy/plugin/actions/GetHintAction.kt")
        assertFalse(source.isEmpty(), "GetHintAction.kt must exist")
        assertTrue(source.contains("executeOnPooledThread"), "actionPerformed must delegate network I/O to a pooled thread")
    }

    @Test
    fun `AcademyToolWindowPanel_init_doesNotBlockOnNetwork`() {
        val source = readSources("com/javaacademy/plugin/toolwindow/AcademyToolWindowPanel.kt")
        assertFalse(source.isEmpty(), "AcademyToolWindowPanel.kt must exist")
        assertTrue(
            source.contains("tryRestoreSessionAsync") || source.contains("executeOnPooledThread"),
            "Session restore must run on a pooled thread, not blocking the EDT in init"
        )
        assertFalse(
            source.contains("authClient.refresh") && !source.contains("executeOnPooledThread"),
            "authClient.refresh must only be called from inside executeOnPooledThread"
        )
    }

    @Test
    fun `CourseTreePanel_loadCourses_runsOnPooledThread`() {
        val source = readSources("com/javaacademy/plugin/toolwindow/CourseTreePanel.kt")
        assertFalse(source.isEmpty(), "CourseTreePanel.kt must exist")
        assertTrue(source.contains("executeOnPooledThread"), "loadCourses must run network I/O on a pooled thread")
    }

    private fun containsDirectNetworkCallInActionPerformed(source: String): Boolean {
        val actionPerformedBlock = source
            .substringAfter("override fun actionPerformed")
            .substringBefore("\n    override")
        return actionPerformedBlock.contains("httpClient.newCall") ||
            actionPerformedBlock.contains(".execute()")
    }
}
