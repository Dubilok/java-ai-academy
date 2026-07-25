package com.javaacademy.plugin.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.javaacademy.plugin.actions.GetHintAction
import com.javaacademy.plugin.actions.StartTaskAction
import com.javaacademy.plugin.actions.VerifyTaskAction
import com.javaacademy.plugin.api.ApiClient
import com.javaacademy.plugin.api.CourseStub
import com.javaacademy.plugin.api.TaskStub
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.event.TreeSelectionEvent
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
    private val startTaskButton = JButton("Start Task").apply { isEnabled = false }
    private val verifyButton = JButton("Verify").apply { isEnabled = false }
    private val hintButton = JButton("Hint").apply { isEnabled = false }

    init {
        border = JBUI.Borders.empty(4)
        TreeSpeedSearch.installOn(tree)
        tree.isRootVisible = false
        tree.showsRootHandles = true

        tree.addTreeSelectionListener { event: TreeSelectionEvent ->
            val selectedNode = event.newLeadSelectionPath
                ?.lastPathComponent as? DefaultMutableTreeNode
            val taskNode = selectedNode?.userObject as? TaskNode
            startTaskButton.isEnabled = taskNode != null
            verifyButton.isEnabled = taskNode != null
            hintButton.isEnabled = taskNode != null
            startTaskButton.text = if (taskNode != null) "Start: ${taskNode.task.title}" else "Start Task"
        }

        startTaskButton.addActionListener {
            val selectedNode = tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode
            val taskNode = selectedNode?.userObject as? TaskNode ?: return@addActionListener
            dispatchAction(StartTaskAction(taskNode.task.id, taskNode.task.title, apiClient))
        }

        verifyButton.addActionListener {
            val selectedNode = tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode
            val taskNode = selectedNode?.userObject as? TaskNode ?: return@addActionListener
            dispatchAction(VerifyTaskAction(taskNode.task.id, taskNode.task.title, apiClient))
        }

        hintButton.addActionListener {
            val selectedNode = tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode
            val taskNode = selectedNode?.userObject as? TaskNode ?: return@addActionListener
            dispatchAction(GetHintAction(taskNode.task.id, taskNode.task.title, apiClient))
        }

        val topBar = JPanel(BorderLayout())
        val rightButtons = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0))
        val refreshButton = JButton("Refresh")
        refreshButton.addActionListener { loadCourses() }
        rightButtons.add(hintButton)
        rightButtons.add(verifyButton)
        rightButtons.add(startTaskButton)
        rightButtons.add(refreshButton)
        topBar.add(rightButtons, BorderLayout.EAST)

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

    private fun dispatchAction(action: com.intellij.openapi.actionSystem.AnAction) {
        val dataContext = com.intellij.openapi.actionSystem.DataContext { dataId ->
            when (dataId) {
                com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT.name -> project
                else -> null
            }
        }
        val event = com.intellij.openapi.actionSystem.AnActionEvent.createFromDataContext(
            "AcademyToolWindow", null, dataContext
        )
        action.actionPerformed(event)
    }
}
