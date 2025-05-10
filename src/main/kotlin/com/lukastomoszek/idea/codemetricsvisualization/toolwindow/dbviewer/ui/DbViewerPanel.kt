package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.dbviewer.ui

import com.intellij.icons.AllIcons
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
import java.awt.BorderLayout
import java.awt.event.ActionListener
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
        val enterKeyListener = ActionListener { loadTableData() }
        sqlQueryField.addActionListener(enterKeyListener)

        val controlPanel = createControlPanel().apply {
            border = JBUI.Borders.emptyBottom(5)
        }

        setupTableComboBoxListeners()

        val contentPanel = JPanel(BorderLayout()).apply {
            table.isStriped = true
            table.emptyText.text = "No data to display or table not loaded"

            add(controlPanel, BorderLayout.NORTH)
            add(JBScrollPane(table), BorderLayout.CENTER)
            add(statusLabel, BorderLayout.SOUTH)
            border = JBUI.Borders.empty(5, 10, 0, 10)
        }

        setContent(contentPanel)
        updateTableList()
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

    private fun setupTableComboBoxListeners() {
        tableComboBox.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                val selectedTable = e.item as? String
                if (selectedTable != null) {
                    updateSqlQueryFieldAndLoadData(selectedTable)
                } else {
                    tableModel.clearData()
                    statusLabel.text = "Please select a table."
                    sqlQueryField.text = ""
                }
            }
        }

        tableComboBox.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                updateTableList()
            }

            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {}
            override fun popupMenuCanceled(e: PopupMenuEvent?) {}
        })
    }

    private fun updateSqlQueryFieldAndLoadData(tableName: String) {
        statusLabel.text = "Fetching columns for '$tableName'..."
        DbViewerService.getInstance(project).getTableColumns(tableName) { result ->
            val queryTemplate = result.fold(
                onSuccess = { columns ->
                    val columnString = if (columns.isEmpty()) "*" else columns.joinToString(", ") { "\"$it\"" }
                    "SELECT $columnString FROM \"$tableName\" LIMIT $ROW_LIMIT"
                },
                onFailure = {
                    statusLabel.text = "Error fetching columns for '$tableName', using default query."
                    thisLogger().warn("Error fetching columns for $tableName: ${it.message}", it)
                    "SELECT * FROM \"$tableName\" LIMIT $ROW_LIMIT"
                }
            )
            sqlQueryField.text = queryTemplate
            loadTableData()
        }
    }

    fun refreshPanelOnShow() = updateTableList()

    private fun updateTableList() {
        val previouslySelectedItem = tableComboBox.selectedItem as? String
        statusLabel.text = "Loading table list..."

        DbViewerService.getInstance(project).getTableNames { result ->
            result.fold(
                onSuccess = { tableNames ->
                    if (tableListModel.items == tableNames && tableListModel.selectedItem == previouslySelectedItem) {
                        if (tableListModel.isEmpty) {
                            statusLabel.text = "No tables found in the database."
                        }
                        return@fold
                    }

                    tableListModel.removeAll()
                    tableListModel.add(tableNames)

                    tableListModel.selectedItem = when {
                        previouslySelectedItem != null && tableNames.contains(previouslySelectedItem) -> previouslySelectedItem
                        else -> tableNames.firstOrNull()
                    }

                    val isTableSelected = tableListModel.selectedItem != null

                    if (!isTableSelected) {
                        statusLabel.text =
                            if (tableNames.isEmpty()) "No tables found in the database." else "Select a table."
                        tableModel.clearData()
                    } else {
                        if (tableComboBox.selectedItem == null && tableNames.isNotEmpty()) {
                            tableComboBox.selectedItem = tableNames.first()
                        }
                        if (sqlQueryField.text.isBlank() && tableComboBox.selectedItem != null) {
                            updateSqlQueryFieldAndLoadData(tableComboBox.selectedItem as String)
                        } else if (tableComboBox.selectedItem != null) {
                            statusLabel.text =
                                "Table '${tableComboBox.selectedItem}' selected. Modify query or execute."
                        }
                    }
                },
                onFailure = {
                    statusLabel.text = "Error loading table list: ${it.message}"
                    tableListModel.removeAll()
                    tableModel.clearData()
                }
            )
        }
    }

    private fun loadTableData() {
        val sqlQuery = sqlQueryField.text.trim()
        val selectedTable = tableComboBox.selectedItem as? String

        if (sqlQuery.isBlank()) {
            statusLabel.text = "SQL Query cannot be empty."
            tableModel.clearData()
            return
        }

        statusLabel.text = "Executing query for table '${selectedTable ?: "selected source"}'..."
        tableModel.clearData()

        DbViewerService.getInstance(project).queryTableData(sqlQuery) { result ->
            handleQueryResponse(result, sqlQuery, selectedTable)
        }
    }

    private fun handleQueryResponse(result: Result<QueryResult>, sqlQuery: String, tableName: String?) {
        result.fold(
            onSuccess = { queryResult ->
                val rows = queryResult.rows.map { row ->
                    queryResult.columnNames.map { row[it] }.toTypedArray()
                }
                tableModel.setData(
                    queryResult.columnNames.toTypedArray(),
                    queryResult.columnTypes.toTypedArray(),
                    rows
                )
                updateStatusLabelFromResult(queryResult, tableName)
            },
            onFailure = { error ->
                val context = tableName?.let { "for table '$it'" } ?: ""
                statusLabel.text = "Error querying $context: ${error.message}"
                tableModel.clearData()
                thisLogger().warn(
                    "DB Viewer Error querying $context with SQL: $sqlQuery : ${error.message}",
                    error
                )
            }
        )
    }

    private fun updateStatusLabelFromResult(queryResult: QueryResult, tableName: String?) {
        val context = tableName?.let { "Table '$it'" } ?: "Query result"
        statusLabel.text = "$context: Displaying ${queryResult.rows.size} row(s)."
    }

    companion object {
        const val ROW_LIMIT = 100
    }
}
