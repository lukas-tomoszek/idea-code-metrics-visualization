package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.dbviewer.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.lukastomoszek.idea.codemetricsvisualization.config.ui.DataSourceConfigurable
import com.lukastomoszek.idea.codemetricsvisualization.db.model.QueryResult
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.dbviewer.service.DbViewerService
import kotlinx.coroutines.CancellationException
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

class DbViewerPanel(private val project: Project) : SimpleToolWindowPanel(true, true) {

    private val tableModel = DbViewerTableModel()
    private val table = JBTable(tableModel)
    private val tableListModel = CollectionComboBoxModel<String>()
    private val tableComboBox = ComboBox(tableListModel)
    private val sqlQueryField = JBTextField()
    private val statusLabel = JBLabel("Select a table to view data.", SwingConstants.CENTER)
    private val executeQueryButton = JButton(AllIcons.Actions.Execute).apply {
        toolTipText = "Execute SQL Query"
        addActionListener { loadTableData() }
    }
    private val openDataSourceSettingsButton = JButton(AllIcons.General.Settings).apply {
        toolTipText = "Open Data Source Settings"
        addActionListener {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, DataSourceConfigurable::class.java)
        }
    }

    init {
        setupListeners()
        setContent(createContentPanel())
        updateTableList()
    }

    fun refreshPanelOnShow() = updateTableList()

    private fun setupListeners() {
        sqlQueryField.addActionListener { loadTableData() }

        tableComboBox.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                (e.item as? String)?.let(::selectTable) ?: run {
                    tableModel.clearData()
                    statusLabel.text = "Please select a table."
                    sqlQueryField.text = ""
                }
            }
        }

        tableComboBox.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) = updateTableList()
            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {}
            override fun popupMenuCanceled(e: PopupMenuEvent?) {}
        })
    }

    private fun createContentPanel(): JPanel = JPanel(BorderLayout()).apply {
        table.isStriped = true
        table.emptyText.text = "No data to display or table not loaded"
        add(createControlPanel(), BorderLayout.NORTH)
        add(JBScrollPane(table), BorderLayout.CENTER)
        add(statusLabel, BorderLayout.SOUTH)
        border = JBUI.Borders.empty(5, 10, 0, 10)
    }

    private fun createControlPanel(): JPanel = panel {
        row("Table:") {
            cell(tableComboBox).resizableColumn().align(AlignX.FILL)
            cell(openDataSourceSettingsButton)
        }
        row("SQL Query:") {
            cell(sqlQueryField).resizableColumn().align(AlignX.FILL)
            cell(executeQueryButton)
        }
    }

    private fun updateTableList() {
        val previous = tableComboBox.selectedItem as? String
        DbViewerService.getInstance(project).getTableNames { result ->
            result.fold(
                onSuccess = { names ->
                    if (tableListModel.items == names && tableListModel.selectedItem == previous) {
                        if (names.isEmpty()) statusLabel.text = "No tables found in the database."
                        return@fold
                    }
                    tableListModel.removeAll()
                    tableListModel.add(names)
                    tableListModel.selectedItem = when {
                        previous != null && names.contains(previous) -> previous
                        else -> names.firstOrNull()
                    }
                },
                onFailure = { error ->
                    if (error is ControlFlowException || error is CancellationException) throw error
                    setErrorStatus("Error loading table list: ${error.message}")
                }
            )
        }
    }

    private fun selectTable(tableName: String) {
        statusLabel.text = "Fetching columns for '$tableName'..."
        DbViewerService.getInstance(project).getTableColumns(tableName) { result ->
            val query = result.fold(
                onSuccess = { columns ->
                    val cols = if (columns.isEmpty()) "*" else columns.joinToString(", ") { "\"$it\"" }
                    "SELECT $cols FROM \"$tableName\" LIMIT $ROW_LIMIT"
                },
                onFailure = { error ->
                    if (error is ControlFlowException || error is CancellationException) throw error
                    statusLabel.text = "Error fetching columns for '$tableName', using default query."
                    thisLogger().warn("Error fetching columns for $tableName: ${error.message}", error)
                    "SELECT * FROM \"$tableName\" LIMIT $ROW_LIMIT"
                }
            )
            sqlQueryField.text = query
            loadTableData()
        }
    }

    private fun loadTableData() {
        val query = sqlQueryField.text.trim()
        val table = tableComboBox.selectedItem as? String
        if (query.isBlank()) return setErrorStatus("SQL Query cannot be empty.")
        statusLabel.text = "Executing query for table '${table ?: "selected source"}'..."
        tableModel.clearData()
        DbViewerService.getInstance(project).queryTableData(query) { dataResult ->
            if (table != null) {
                DbViewerService.getInstance(project).getTableRowCount(table) {
                    handleQueryResponse(dataResult, it.getOrNull(), query, table)
                }
            } else handleQueryResponse(dataResult, null, query, table)
        }
    }

    private fun handleQueryResponse(result: Result<QueryResult>, total: Long?, sql: String, table: String?) {
        result.fold(
            onSuccess = {
                val rows = it.rows.map { row -> it.columnNames.map { col -> row[col] }.toTypedArray() }
                tableModel.setData(it.columnNames.toTypedArray(), it.columnTypes.toTypedArray(), rows)
                val ctx = table?.let { tbl -> "Table '$tbl'" } ?: "Query result"
                statusLabel.text = if (total != null) "$ctx: Displaying ${rows.size} out of $total row(s)."
                else "$ctx: Displaying ${rows.size} row(s)."
            },
            onFailure = {
                if (it is ControlFlowException || it is CancellationException) throw it
                setErrorStatus("Error querying ${table ?: ""}: ${it.message}")
                thisLogger().warn("Query failed: $sql", it)
            }
        )
    }

    private fun setErrorStatus(text: String, clear: Boolean = true) {
        statusLabel.text = text
        if (clear) tableModel.clearData()
    }

    companion object {
        const val ROW_LIMIT = 100
    }
}
