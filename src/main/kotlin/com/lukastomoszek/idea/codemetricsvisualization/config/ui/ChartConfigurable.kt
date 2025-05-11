package com.lukastomoszek.idea.codemetricsvisualization.config.ui

import com.intellij.openapi.project.Project
import com.intellij.util.ui.ColumnInfo
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.ChartSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ChartConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DefaultChartConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog.AbstractDialog
import com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog.ChartDialog

class ChartConfigurable(project: Project) :
    AbstractListConfigurable<ChartConfig>(
        project,
        "Charts",
        "Charts",
        noItemsText = "No Chart configurations defined",
        commentText = "Define named SQL queries for generating charts in the tool window."
    ) {

    private val nameColumn = object : ColumnInfo<ChartConfig, String>("Name") {
        override fun valueOf(item: ChartConfig): String = item.name
    }

    private val sqlColumn = object : ColumnInfo<ChartConfig, String>("SQL Template") {
        override fun valueOf(item: ChartConfig): String = item.sql
            .replace(Regex("\\s+"), " ")
    }

    override fun getColumnInfos(): Array<ColumnInfo<ChartConfig, *>> = arrayOf(nameColumn, sqlColumn)

    override fun createNewItem(): ChartConfig = ChartConfig(name = DefaultChartConfig.NAME)

    override fun createEditDialog(item: ChartConfig): AbstractDialog<ChartConfig> {
        val otherNames = items.filterNot { it === item }.map { it.name }
        return ChartDialog(project, item, otherNames)
    }

    override fun addItem() {
        val newItem = createNewItem()
        val currentNames = items.map { it.name }
        val dialog = ChartDialog(project, newItem.copy(), currentNames)
        if (dialog.showAndGet()) {
            items.add(dialog.getUpdatedConfig())
            tableModel.fireTableDataChanged()
        }
    }

    override fun getItemsFromSettings(): List<ChartConfig> = ChartSettings.getInstance(project).state.charts

    override fun saveItemsToSettings(items: List<ChartConfig>) {
        ChartSettings.getInstance(project).state.charts = items.toMutableList()
    }

    override fun copyItem(item: ChartConfig): ChartConfig = item.copy()
}
