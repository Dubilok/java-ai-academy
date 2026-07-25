package com.javaacademy.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class VerifyTaskAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        Messages.showInfoMessage(
            event.project,
            "Sign in via the Java AI Academy tool window to verify your solution.",
            "Java AI Academy"
        )
    }
}
