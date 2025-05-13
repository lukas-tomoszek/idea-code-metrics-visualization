package com.lukastomoszek.idea.codemetricsvisualization.config.ui

import com.intellij.openapi.project.Project
import com.intellij.util.ui.ColumnInfo
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.ChartSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ChartConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog.AbstractNamedDialog
import com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog.ChartDialog

class ChartConfigurable(project: Project) :
    AbstractListNamedConfigurable<ChartConfig>(
        project,
        "Charts",
        "Charts",
        noItemsText = "No Chart configurations defined",
        commentText = "Define named SQL queries for generating charts in the tool window."
    ) {

    private val sqlColumn = object : ColumnInfo<ChartConfig, String>("SQL Template") {
        override fun valueOf(item: ChartConfig): String = item.sql
            .replace(Regex("\\s+"), " ")
    }

    override fun getColumnInfos(): Array<ColumnInfo<ChartConfig, *>> = arrayOf(nameColumn, sqlColumn)

    override fun createNewItem(): ChartConfig = ChartConfig()

    override fun createEditDialog(item: ChartConfig): AbstractNamedDialog<ChartConfig> {
        val otherNames = items.filterNot { it === item }.map { it.name }
        return ChartDialog(project, item.copy(), otherNames)
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

    override fun getItemsFromSettings(): List<ChartConfig> = ChartSettings.getInstance(project).state.configs

    override fun saveItemsToSettings(items: List<ChartConfig>) {
        ChartSettings.getInstance(project).update(items)
    }

    override fun copyItem(item: ChartConfig): ChartConfig = item.copy()
}
