package com.lukastomoszek.idea.codemetricsvisualization.config.ui

import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import com.lukastomoszek.idea.codemetricsvisualization.config.state.NamedConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog.AbstractNamedDialog
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

abstract class AbstractListNamedConfigurable<ConfigType : NamedConfig>(
    protected val project: Project,
    displayName: String,
    helpTopic: String,
    private val noItemsText: String = "No items configured",
    private val commentText: String = ""
) : BoundSearchableConfigurable(displayName, helpTopic), Configurable.NoScroll {

    protected val items = mutableListOf<ConfigType>()
    protected lateinit var tableModel: ListTableModel<ConfigType>
    protected lateinit var table: JBTable

    protected abstract fun getColumnInfos(): Array<ColumnInfo<ConfigType, *>>
    protected abstract fun createNewItem(): ConfigType
    protected abstract fun createEditDialog(item: ConfigType): AbstractNamedDialog<ConfigType>
    protected abstract fun getItemsFromSettings(): List<ConfigType>
    protected abstract fun saveItemsToSettings(items: List<ConfigType>)
    protected abstract fun copyItem(item: ConfigType): ConfigType

    override fun createPanel(): DialogPanel {
        loadItemsFromSettings()

        tableModel = ListTableModel(getColumnInfos(), items, 0)
        table = JBTable(tableModel)
        table.setShowGrid(false)
        table.emptyText.text = noItemsText

        updateTable(table)

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2 && table.selectedRow >= 0) {
                    editItem()
                }
            }
        })

        val toolbarDecorator = ToolbarDecorator.createDecorator(table)
            .setAddAction { addItem() }
            .setEditAction { editItem() }
            .setRemoveAction { removeItem() }

        addExtraToolbarActions(toolbarDecorator)

        return panel {
            row {
                cell(toolbarDecorator.createPanel())
                    .align(Align.FILL)
            }.resizableRow()
            if (commentText.isNotBlank()) {
                row {
                    comment(commentText)
                }
            }
        }
    }

    protected val nameColumn = object : ColumnInfo<ConfigType, String>("Name") {
        override fun valueOf(item: ConfigType): String = item.name
    }

    protected open fun updateTable(table: JBTable) {}

    protected open fun addExtraToolbarActions(decorator: ToolbarDecorator) {}

    protected open fun addItem() {
        val newItem = createNewItem()
        val dialog = createEditDialog(newItem)
        if (dialog.showAndGet()) {
            items.add(dialog.getUpdatedConfig())
            tableModel.fireTableDataChanged()
        }
    }

    protected open fun editItem() {
        val selectedRow = table.selectedRow
        if (selectedRow < 0 || selectedRow >= items.size) {
            return
        }
        val currentItem = items[selectedRow]
        val itemCopy = copyItem(currentItem)
        val dialog = createEditDialog(itemCopy)

        if (dialog.showAndGet()) {
            items[selectedRow] = dialog.getUpdatedConfig()
            tableModel.fireTableRowsUpdated(selectedRow, selectedRow)
        }
    }

    protected open fun removeItem() {
        val selectedRow = table.selectedRow
        if (selectedRow >= 0 && selectedRow < items.size) {
            items.removeAt(selectedRow)
            tableModel.fireTableDataChanged()
        }
    }

    override fun isModified(): Boolean {
        val originalItems = getItemsFromSettings()
        return items != originalItems
    }

    override fun apply() {
        saveItemsToSettings(items.map { copyItem(it) })
    }

    override fun reset() {
        loadItemsFromSettings()
        tableModel.fireTableDataChanged()
    }

    private fun loadItemsFromSettings() {
        items.clear()
        items.addAll(getItemsFromSettings().map { copyItem(it) })
    }
}
