package com.javaacademy.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class StartTaskAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        Messages.showInfoMessage(
            event.project,
            "Sign in via the Java AI Academy tool window to start a task.",
            "Java AI Academy"
        )
    }
}
