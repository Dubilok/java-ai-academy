package com.javaacademy.plugin.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.javaacademy.plugin.api.ApiClient
import com.javaacademy.plugin.api.CourseStub
import com.javaacademy.plugin.api.TaskStub
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

data class CourseNode(val course: CourseStub) {
    override fun toString(): String = "${course.title} [${course.technology}]"
}

data class TaskNode(val task: TaskStub) {
    override fun toString(): String = "${task.title}  (${task.difficulty}, ${task.xpReward} XP)"
}

class CourseTreePanel(private val project: Project, private val apiClient: ApiClient) :
    JBPanel<CourseTreePanel>(BorderLayout()) {

    private val rootNode = DefaultMutableTreeNode("Courses")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel)
    private val statusLabel = JBLabel("Loading courses…", SwingConstants.CENTER)

    init {
        border = JBUI.Borders.empty(4)
        TreeSpeedSearch.installOn(tree)
        tree.isRootVisible = false
        tree.showsRootHandles = true

        val topBar = JPanel(FlowLayout(FlowLayout.RIGHT))
        val refreshButton = JButton("Refresh")
        refreshButton.addActionListener { loadCourses() }
        topBar.add(refreshButton)

        add(topBar, BorderLayout.NORTH)
        add(statusLabel, BorderLayout.CENTER)

        loadCourses()
    }

    private fun loadCourses() {
        statusLabel.text = "Loading…"
        remove(JBScrollPane(tree))
        add(statusLabel, BorderLayout.CENTER)
        revalidate()
        repaint()

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val courses = apiClient.fetchBootstrap()
                ApplicationManager.getApplication().invokeLater {
                    populateTree(courses)
                }
            } catch (exception: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    statusLabel.text = "Failed to load: ${exception.message}"
                }
            }
        }
    }

    private fun populateTree(courses: List<CourseStub>) {
        rootNode.removeAllChildren()
        for (course in courses) {
            val courseNode = DefaultMutableTreeNode(CourseNode(course))
            for (task in course.tasks) {
                courseNode.add(DefaultMutableTreeNode(TaskNode(task)))
            }
            rootNode.add(courseNode)
        }
        treeModel.reload()

        remove(statusLabel)
        val scrollPane = JBScrollPane(tree)
        add(scrollPane, BorderLayout.CENTER)
        revalidate()
        repaint()

        if (courses.isEmpty()) {
            statusLabel.text = "No courses found."
            remove(scrollPane)
            add(statusLabel, BorderLayout.CENTER)
        }
    }
}
