package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.lukastomoszek.idea.codemetricsvisualization.db.model.QueryResult
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.controller.ChartController
import org.knowm.xchart.CategoryChart
import org.knowm.xchart.XChartPanel
import java.awt.BorderLayout
import kotlin.coroutines.cancellation.CancellationException

class ChartViewerPanel(project: Project) : SimpleToolWindowPanel(true, true), Disposable {

    private var xChartPanel: XChartPanel<*>? = null
    private val chartPanelContainer = JBUI.Panels.simplePanel()
    private var statusLabel: JBLabel

    private val controller: ChartController = ChartController(project, this)

    init {
        Disposer.register(this, controller)

        val controlsProvider = controller.controlsProvider
        val controlPanel = controlsProvider.createControlsPanel().apply {
            border = JBUI.Borders.empty(5, 10, 0, 10)
        }
        statusLabel = controlsProvider.createStatusLabel()

        setContent(
            JBUI.Panels.simplePanel(chartPanelContainer)
                .addToTop(controlPanel)
                .addToBottom(statusLabel)
        )
        controller.initialize()
    }

    fun updateChart(queryResult: QueryResult, chartName: String) {
        clearChartPanel()

        val (labels, values) = processQueryResult(queryResult)
        if (labels.isEmpty() || values.isEmpty() || (values.firstOrNull()?.isEmpty() == true)) {
            statusLabel.text = "No data to display for '$chartName'."
            return
        }

        try {
            val chart = CategoryChart(1, 1)
            ChartXChartConfigurator.configureChartStyles(chart, labels, values)
            chart.addSeries(chartName, labels, values.first())

            val newChartPanel = XChartPanel(chart)
            xChartPanel = newChartPanel
            chartPanelContainer.add(newChartPanel, BorderLayout.CENTER)
            chartPanelContainer.revalidate()
            chartPanelContainer.repaint()
            statusLabel.text = "Chart '$chartName' loaded with ${queryResult.rows.size} data points."
        } catch (e: Exception) {
            if (e is ControlFlowException || e is CancellationException) throw e
            thisLogger().error("Error rendering chart '$chartName'", e)
            setStatus("Error rendering chart '$chartName': ${e.message}")
            clearChartPanel()
        }
    }

    private fun processQueryResult(queryResult: QueryResult): Pair<List<Any>, List<List<Number>>> {
        if (queryResult.rows.isEmpty() || queryResult.columnNames.size < 2) {
            return Pair(emptyList(), emptyList())
        }

        val labelCol = queryResult.columnNames[0]
        val valueCol = queryResult.columnNames[1]

        val labels = queryResult.rows.map { row ->
            ChartXChartConfigurator.convertToDateCompatible(row[labelCol])
        }
        val values: List<List<Number>> = listOf(queryResult.rows.map {
            (it[valueCol] as? Number)?.toDouble() ?: 0.0
        })

        return Pair(labels, values)
    }

    fun clearChartPanel() {
        xChartPanel?.let {
            chartPanelContainer.remove(it)
            xChartPanel = null
            chartPanelContainer.revalidate()
            chartPanelContainer.repaint()
        }
    }

    fun setStatus(text: String) {
        statusLabel.text = text
    }

    fun refreshPanelOnShow() {
        controller.loadChartConfigurations()
    }

    override fun dispose() {}
}
