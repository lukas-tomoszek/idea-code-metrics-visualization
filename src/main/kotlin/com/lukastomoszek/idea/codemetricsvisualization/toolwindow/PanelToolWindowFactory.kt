package com.lukastomoszek.idea.codemetricsvisualization.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.ui.ChartViewerPanel
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.dbviewer.ui.DbViewerPanel

class PanelToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()

        val chartViewerPanel = ChartViewerPanel(project)
        val chartContent = contentFactory.createContent(chartViewerPanel, "Chart", false)
        chartContent.setDisposer(chartViewerPanel)
        toolWindow.contentManager.addContent(chartContent)

        val dbViewerPanel = DbViewerPanel(project)
        val dbViewerContent = contentFactory.createContent(dbViewerPanel, "DB Viewer", false)
        toolWindow.contentManager.addContent(dbViewerContent)

        toolWindow.contentManager.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                when (val component = event.content.component) {
                    is ChartViewerPanel -> component.refreshPanelOnShow()
                    is DbViewerPanel -> component.refreshPanelOnShow()
                }
            }
        })
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
