package com.javaacademy.plugin.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.javaacademy.plugin.api.ApiClient
import com.javaacademy.plugin.auth.TokenStore

class VerifyTaskAction(
    private val taskId: String,
    private val taskTitle: String,
    private val apiClient: ApiClient
) : AnAction("Verify: $taskTitle") {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (!TokenStore.isAuthenticated()) {
            Messages.showWarningDialog(project, "Sign in via the Java AI Academy tool window first.", "Java AI Academy")
            return
        }

        val editor = event.getData(CommonDataKeys.EDITOR)
        val sourceCode = editor?.document?.text
            ?: readFromOpenEditor(project)
            ?: run {
                Messages.showWarningDialog(
                    project,
                    "Open Solution.java in the editor before verifying.",
                    "Java AI Academy"
                )
                return
            }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val submissionId = apiClient.submitSolution(taskId, sourceCode)
                pollForVerdict(project, submissionId)
            } catch (exception: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "Submission failed: ${exception.message}",
                        "Java AI Academy"
                    )
                }
            }
        }
    }

    private fun readFromOpenEditor(project: com.intellij.openapi.project.Project): String? {
        val editorManager = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
        val selectedFile = editorManager.selectedFiles.firstOrNull() ?: return null
        val doc = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
            .getDocument(selectedFile) ?: return null
        return doc.text
    }

    private fun pollForVerdict(project: com.intellij.openapi.project.Project, submissionId: String) {
        val maxAttempts = 30
        val pollIntervalMs = 1_000L
        for (attempt in 1..maxAttempts) {
            Thread.sleep(pollIntervalMs)
            val verdict = try {
                apiClient.pollSubmission(submissionId)
            } catch (exception: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(project, "Polling failed: ${exception.message}", "Java AI Academy")
                }
                return
            }
            when (verdict.status) {
                "PASSED" -> {
                    ApplicationManager.getApplication().invokeLater {
                        showVerdictNotification(project, "✅ PASSED — $taskTitle", NotificationType.INFORMATION)
                    }
                    return
                }
                "FAILED" -> {
                    val logs = verdict.logs?.take(500) ?: ""
                    ApplicationManager.getApplication().invokeLater {
                        showVerdictNotification(
                            project,
                            "❌ FAILED — $taskTitle\n$logs",
                            NotificationType.WARNING
                        )
                    }
                    return
                }
                else -> continue
            }
        }
        ApplicationManager.getApplication().invokeLater {
            Messages.showWarningDialog(project, "Verdict timed out after ${maxAttempts}s.", "Java AI Academy")
        }
    }

    private fun showVerdictNotification(
        project: com.intellij.openapi.project.Project,
        content: String,
        type: NotificationType
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Java AI Academy")
            .createNotification(content, type)
            .notify(project)
    }
}
