package com.lukastomoszek.idea.codemetricsvisualization.config.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ToolbarDecorator
import com.intellij.util.ui.ColumnInfo
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.DataSourceSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.service.DataSourceService
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DataSourceConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ImportMode
import com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog.AbstractDialog
import com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog.DataSourceDialog

class DataSourceConfigurable(project: Project) :
    AbstractListConfigurable<DataSourceConfig>(
        project,
        "Data Source",
        "DataSource",
        noItemsText = "No Data Sources configured",
        commentText = "Define named SQL configurations to import data into the project's DuckDB database."
    ) {

    private val nameColumn = object : ColumnInfo<DataSourceConfig, String>("Name") {
        override fun valueOf(item: DataSourceConfig): String = item.name
    }

    private val tableColumn = object : ColumnInfo<DataSourceConfig, String>("Table Name") {
        override fun valueOf(item: DataSourceConfig): String = item.tableName
    }

    private val pathColumn = object : ColumnInfo<DataSourceConfig, String>("Input File Path") {
        override fun valueOf(item: DataSourceConfig): String = item.filePath
    }

    private val importModeColumn = object : ColumnInfo<DataSourceConfig, ImportMode>("Mode") {
        override fun valueOf(item: DataSourceConfig): ImportMode = item.importMode
    }

    private val lastImportedAtColumn = object : ColumnInfo<DataSourceConfig, String>("Last Imported At") {
        override fun valueOf(item: DataSourceConfig): String = item.getFormattedLastImportedAt()
    }

    override fun getColumnInfos(): Array<ColumnInfo<DataSourceConfig, *>> =
        arrayOf(nameColumn, tableColumn, pathColumn, importModeColumn, lastImportedAtColumn)

    override fun createNewItem(): DataSourceConfig = DataSourceConfig()

    override fun createEditDialog(item: DataSourceConfig): AbstractDialog<DataSourceConfig> {
        val otherNames = items.filterNot { it === item }.map { it.name }
        return DataSourceDialog(project, item.copy(), otherNames)
    }

    override fun addItem() {
        val newItem = createNewItem()
        val currentNames = items.map { it.name }
        val dialog = DataSourceDialog(project, newItem, currentNames)
        if (dialog.showAndGet()) {
            items.add(dialog.getUpdatedConfig())
            tableModel.fireTableDataChanged()
        }
    }

    override fun getItemsFromSettings(): List<DataSourceConfig> =
        DataSourceSettings.getInstance(project).state.dataSources

    override fun saveItemsToSettings(items: List<DataSourceConfig>) {
        DataSourceSettings.getInstance(project).state.dataSources = items.toMutableList()
    }

    override fun copyItem(item: DataSourceConfig): DataSourceConfig = item.copy()

    override fun addExtraToolbarActions(decorator: ToolbarDecorator) {
        val importAction = object : DumbAwareAction(
            "Run Import SQL Now",
            "Execute the selected data source import SQL",
            AllIcons.Actions.Execute
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                val selectedRow = table.selectedRow
                if (selectedRow >= 0 && selectedRow < items.size) {
                    val itemToImport = items[selectedRow]
                    DataSourceService.getInstance(project).executeImport(itemToImport) {
                        tableModel.fireTableRowsUpdated(selectedRow, selectedRow)
                    }
                }
            }

            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = table.selectedRowCount == 1
            }

            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
        }

        val dropTableAction = object : DumbAwareAction(
            "Drop Table",
            "Drop the database table associated with this data source",
            AllIcons.Actions.Cancel
        ) {
            override fun actionPerformed(e: AnActionEvent) {
                val selectedRow = table.selectedRow
                if (selectedRow >= 0 && selectedRow < items.size) {
                    val item = items[selectedRow]
                    val confirmation = Messages.showYesNoDialog(
                        project,
                        "Are you sure you want to drop the table '${item.tableName}'? This cannot be undone.",
                        "Drop Table Confirmation",
                        Messages.getWarningIcon()
                    )
                    if (confirmation == Messages.YES) {
                        DataSourceService.getInstance(project).dropTable(item.tableName)
                    }
                }
            }

            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled =
                    table.selectedRowCount == 1 && items.getOrNull(table.selectedRow)?.tableName?.isNotBlank() == true
            }

            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
        }

        decorator.addExtraAction(importAction)
        decorator.addExtraAction(dropTableAction)
    }

    override fun removeItem() {
        val selectedRow = table.selectedRow
        if (selectedRow >= 0 && selectedRow < items.size) {
            val itemToRemove = items[selectedRow]
            val tableName = itemToRemove.tableName
            val confirmation = Messages.showYesNoDialog(
                project,
                "Are you sure you want to delete the data source configuration '${itemToRemove.name}'?\nThis will also attempt to drop the associated table '$tableName' if the table name is not blank.",
                "Delete Data Source Confirmation",
                Messages.getWarningIcon()
            )
            if (confirmation == Messages.YES) {
                if (tableName.isNotBlank()) {
                    DataSourceService.getInstance(project).dropTable(tableName) {
                        super.removeItem()
                    }
                } else {
                    super.removeItem()
                }
            }
        }
    }
}
