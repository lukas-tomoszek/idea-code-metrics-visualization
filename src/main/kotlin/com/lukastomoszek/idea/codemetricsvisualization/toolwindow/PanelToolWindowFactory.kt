/*
 * Copyright (c) 2025 Lukáš Tomoszek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
