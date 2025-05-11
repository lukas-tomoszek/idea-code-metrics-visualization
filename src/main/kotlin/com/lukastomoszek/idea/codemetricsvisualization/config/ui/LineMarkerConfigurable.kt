package com.lukastomoszek.idea.codemetricsvisualization.config.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.BooleanTableCellEditor
import com.intellij.ui.BooleanTableCellRenderer
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ColumnInfo
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.LineMarkerSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DefaultLineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog.AbstractDialog
import com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog.LineMarkerDialog
import javax.swing.table.TableCellRenderer

class LineMarkerConfigurable(project: Project) :
    AbstractListConfigurable<LineMarkerConfig>(
        project,
        "Line Markers",
        "Metric Visualization",
        noItemsText = "No Line Marker configurations defined",
        commentText = "Define named configurations for SQL queries and coloring rules used for line markers."
    ) {

    private val enabledColumn = object : ColumnInfo<LineMarkerConfig, Boolean>("Enabled") {
        override fun valueOf(item: LineMarkerConfig): Boolean = item.enabled

        override fun setValue(item: LineMarkerConfig, value: Boolean) {
            item.enabled = value
            val rowIndex = items.indexOf(item)
            if (rowIndex != -1) {
                tableModel.fireTableRowsUpdated(rowIndex, rowIndex)
            }
        }

        override fun isCellEditable(item: LineMarkerConfig): Boolean = true

        override fun getRenderer(item: LineMarkerConfig?): TableCellRenderer {
            return BooleanTableCellRenderer()
        }
    }

    private val nameColumn = object : ColumnInfo<LineMarkerConfig, String>("Name") {
        override fun valueOf(item: LineMarkerConfig): String = item.name
    }

    private val sqlTemplateColumn = object : ColumnInfo<LineMarkerConfig, String>("SQL Template") {
        override fun valueOf(item: LineMarkerConfig): String = item.sqlTemplate
            .replace(Regex("\\s+"), " ")
    }

    private val rulesCountColumn = object : ColumnInfo<LineMarkerConfig, String>("Rules") {
        override fun valueOf(item: LineMarkerConfig): String = "${item.lineMarkerRules.size} rules"
    }

    override fun getColumnInfos(): Array<ColumnInfo<LineMarkerConfig, *>> =
        arrayOf(enabledColumn, nameColumn, sqlTemplateColumn, rulesCountColumn)

    override fun updateTable(table: JBTable) {
        val enabledCol = table.columnModel.getColumn(0)
        enabledCol.cellRenderer = BooleanTableCellRenderer()
        enabledCol.cellEditor = BooleanTableCellEditor()

        val preferredWidth = 70
        enabledCol.preferredWidth = preferredWidth
        enabledCol.minWidth = preferredWidth
        enabledCol.maxWidth = preferredWidth
    }

    override fun createNewItem(): LineMarkerConfig = LineMarkerConfig(name = DefaultLineMarkerConfig.NAME)

    override fun createEditDialog(item: LineMarkerConfig): AbstractDialog<LineMarkerConfig> =
        LineMarkerDialog(project, item)

    override fun getItemsFromSettings(): List<LineMarkerConfig> =
        LineMarkerSettings.getInstance(project).state.lineMarkerConfigs

    override fun saveItemsToSettings(items: List<LineMarkerConfig>) {
        LineMarkerSettings.getInstance(project).state.lineMarkerConfigs = items.toMutableList()
    }

    override fun copyItem(item: LineMarkerConfig): LineMarkerConfig =
        item.copy(lineMarkerRules = item.lineMarkerRules.map { it.copy() }.toMutableList())
}
