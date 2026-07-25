package com.javaacademy.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages
import com.javaacademy.plugin.api.ApiClient
import com.javaacademy.plugin.auth.TokenStore

class GetHintAction(
    private val taskId: String,
    private val taskTitle: String,
    private val apiClient: ApiClient
) : AnAction("Get Hint: $taskTitle") {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (!TokenStore.isAuthenticated()) {
            Messages.showWarningDialog(project, "Sign in via the Java AI Academy tool window first.", "Java AI Academy")
            return
        }

        val editor = event.getData(CommonDataKeys.EDITOR)
        if (editor == null) {
            Messages.showWarningDialog(project, "Open Solution.java in the editor to get a hint.", "Java AI Academy")
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val hint = apiClient.fetchHint(taskId)
                val formattedHint = formatHintAsComments(hint)

                ApplicationManager.getApplication().invokeLater {
                    WriteCommandAction.runWriteCommandAction(project, "Insert Academy Hint", null, {
                        val caretOffset = editor.caretModel.offset
                        editor.document.insertString(caretOffset, formattedHint)
                    })
                }
            } catch (exception: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    val message = when {
                        exception.message?.contains("429") == true ->
                            "Hint quota reached. Try again later."
                        exception.message?.contains("400") == true ->
                            "Submit your solution first to get a contextual hint."
                        else -> "Failed to get hint: ${exception.message}"
                    }
                    Messages.showWarningDialog(project, message, "Java AI Academy")
                }
            }
        }
    }

    private fun formatHintAsComments(hint: String): String {
        val lines = hint.trim().lines()
        val commentLines = lines.joinToString("\n") { line -> "// $line" }
        return "\n// TODO (Academy Hint):\n$commentLines\n"
    }
}
