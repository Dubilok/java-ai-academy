package com.javaacademy.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtil
import com.javaacademy.plugin.api.ApiClient
import com.javaacademy.plugin.auth.TokenStore

class StartTaskAction(
    private val taskId: String,
    private val taskTitle: String,
    private val apiClient: ApiClient
) : AnAction("Start: $taskTitle") {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        if (!TokenStore.isAuthenticated()) {
            Messages.showWarningDialog(project, "Sign in via the Java AI Academy tool window first.", "Java AI Academy")
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val taskDetail = apiClient.fetchTask(taskId)
                val template = taskDetail.templateCode
                    ?: "// No template available for this task.\npublic class Solution {\n}\n"

                ApplicationManager.getApplication().invokeLater {
                    createSolutionFile(project, template)
                }
            } catch (exception: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "Failed to fetch task template: ${exception.message}",
                        "Java AI Academy"
                    )
                }
            }
        }
    }

    private fun createSolutionFile(project: com.intellij.openapi.project.Project, templateCode: String) {
        val sourceRoot = findSourceRoot(project)
        if (sourceRoot == null) {
            Messages.showErrorDialog(project, "No source root found in this project.", "Java AI Academy")
            return
        }

        WriteCommandAction.runWriteCommandAction(project, "Start Java AI Academy Task", null, {
            try {
                val existing: VirtualFile? = sourceRoot.findChild("Solution.java")
                val file = existing ?: sourceRoot.createChildData(this, "Solution.java")
                VfsUtil.saveText(file, templateCode)
                FileEditorManager.getInstance(project).openFile(file, true)
            } catch (exception: Exception) {
                Messages.showErrorDialog(
                    project,
                    "Failed to create Solution.java: ${exception.message}",
                    "Java AI Academy"
                )
            }
        })
    }

    private fun findSourceRoot(project: com.intellij.openapi.project.Project): VirtualFile? {
        val sourceRoots = ProjectRootManager.getInstance(project).contentSourceRoots
        return sourceRoots.firstOrNull() ?: project.baseDir
    }
}
