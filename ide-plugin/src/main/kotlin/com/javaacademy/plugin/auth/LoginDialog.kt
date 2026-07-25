package com.javaacademy.plugin.auth

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JPanel

class LoginDialog(project: Project?) : DialogWrapper(project) {
    private val emailField = JBTextField(30)
    private val passwordField = JBPasswordField()

    val email: String get() = emailField.text.trim()
    val password: String get() = String(passwordField.password)

    init {
        title = "Sign in to Java AI Academy"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(4, 4, 4, 4)
        }

        constraints.gridx = 0; constraints.gridy = 0; constraints.weightx = 0.0
        panel.add(JBLabel("Email:"), constraints)
        constraints.gridx = 1; constraints.weightx = 1.0
        panel.add(emailField, constraints)

        constraints.gridx = 0; constraints.gridy = 1; constraints.weightx = 0.0
        panel.add(JBLabel("Password:"), constraints)
        constraints.gridx = 1; constraints.weightx = 1.0
        panel.add(passwordField, constraints)

        return panel
    }

    override fun getPreferredFocusedComponent(): JComponent = emailField
}
