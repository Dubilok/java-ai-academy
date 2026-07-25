package com.javaacademy.plugin.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import javax.swing.SwingConstants

class AcademyToolWindowPanel(private val project: Project) : JBPanel<AcademyToolWindowPanel>(BorderLayout()) {

    init {
        val placeholder = JBLabel(
            "<html><center>Java AI Academy<br/><br/>Sign in to browse courses and tasks.</center></html>",
            SwingConstants.CENTER
        )
        add(placeholder, BorderLayout.CENTER)
    }
}
