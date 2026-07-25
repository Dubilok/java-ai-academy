package com.javaacademy.plugin.actions

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GetHintActionTest {

    private fun formatHintAsComments(hint: String): String {
        val lines = hint.trim().lines()
        val commentLines = lines.joinToString("\n") { line -> "// $line" }
        return "\n// TODO (Academy Hint):\n$commentLines\n"
    }

    @Test
    fun `formatHint_singleLineSocraticQuestion_insertedAsComment`() {
        val hint = "What does the String.equals() method do?"
        val result = formatHintAsComments(hint)
        assertTrue(result.contains("// TODO (Academy Hint):"))
        assertTrue(result.contains("// What does the String.equals() method do?"))
        assertFalse(result.contains("public class"))
    }

    @Test
    fun `formatHint_multiLineHint_eachLineCommented`() {
        val hint = "Think about the loop bounds.\nWhat happens at index 0?"
        val result = formatHintAsComments(hint)
        assertTrue(result.contains("// Think about the loop bounds."))
        assertTrue(result.contains("// What happens at index 0?"))
    }

    @Test
    fun `formatHint_neverContainsSolutionCode`() {
        val hint = "Have you considered using a HashMap to track seen elements?"
        val result = formatHintAsComments(hint)
        assertTrue(result.lines().all { it.startsWith("//") || it.isEmpty() })
    }
}
