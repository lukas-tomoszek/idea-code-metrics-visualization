package com.lukastomoszek.idea.codemetricsvisualization.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.dbviewer.ui.DbViewerPanel

class PanelToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        val dbViewerPanel = DbViewerPanel(project)
        val dbViewerContent = contentFactory.createContent(dbViewerPanel, "DB Viewer", false)
        toolWindow.contentManager.addContent(dbViewerContent)

        toolWindow.contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                when (val component = event.content.component) {
                    is DbViewerPanel -> component.refreshPanelOnShow()
                }
            }
        })
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
