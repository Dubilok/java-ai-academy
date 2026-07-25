package com.javaacademy.plugin.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.javaacademy.plugin.api.ApiClient
import com.javaacademy.plugin.auth.AuthClient
import com.javaacademy.plugin.auth.LoginDialog
import com.javaacademy.plugin.auth.TokenStore
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants

class AcademyToolWindowPanel(private val project: Project) : JBPanel<AcademyToolWindowPanel>(BorderLayout()) {

    private val authClient = AuthClient(baseUrl = "http://localhost:8080")
    private val apiClient = ApiClient(baseUrl = "http://localhost:8080")

    init {
        tryRestoreSession()
        refresh()
    }

    private fun tryRestoreSession() {
        val storedRefreshToken = TokenStore.getRefreshToken() ?: return
        try {
            val response = authClient.refresh(storedRefreshToken)
            TokenStore.setTokens(response.accessToken, response.refreshToken)
        } catch (_: Exception) {
            TokenStore.clearTokens()
        }
    }

    private fun refresh() {
        removeAll()
        if (TokenStore.isAuthenticated()) {
            showAuthenticatedView()
        } else {
            showLoginView()
        }
        revalidate()
        repaint()
    }

    private fun showLoginView() {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(20)

        val label = JBLabel(
            "<html><center>Java AI Academy<br/><br/>Sign in to browse courses and tasks.</center></html>",
            SwingConstants.CENTER
        )

        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        val signInButton = JButton("Sign In")
        signInButton.addActionListener { showLoginDialog() }
        buttonPanel.add(signInButton)

        panel.add(label, BorderLayout.CENTER)
        panel.add(buttonPanel, BorderLayout.SOUTH)
        add(panel, BorderLayout.CENTER)
    }

    private fun showAuthenticatedView() {
        val email = TokenStore.extractEmail(TokenStore.getAccessToken() ?: "") ?: "Unknown"
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(4)

        val userLabel = JBLabel("Signed in as: $email", SwingConstants.LEFT)
        val signOutButton = JButton("Sign Out")
        signOutButton.addActionListener {
            TokenStore.clearTokens()
            refresh()
        }

        val topBar = JPanel(BorderLayout())
        topBar.border = JBUI.Borders.emptyBottom(4)
        topBar.add(userLabel, BorderLayout.CENTER)
        topBar.add(signOutButton, BorderLayout.EAST)

        val courseTree = CourseTreePanel(project, apiClient)

        panel.add(topBar, BorderLayout.NORTH)
        panel.add(courseTree, BorderLayout.CENTER)
        add(panel, BorderLayout.CENTER)
    }

    private fun showLoginDialog() {
        val dialog = LoginDialog(project)
        if (!dialog.showAndGet()) return

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val response = authClient.login(dialog.email, dialog.password)
                TokenStore.setTokens(response.accessToken, response.refreshToken)
                ApplicationManager.getApplication().invokeLater { refresh() }
            } catch (exception: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "Sign in failed: ${exception.message}",
                        "Java AI Academy"
                    )
                }
            }
        }
    }
}
