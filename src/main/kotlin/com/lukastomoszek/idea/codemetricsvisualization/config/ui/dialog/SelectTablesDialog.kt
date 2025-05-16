package com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CheckBoxList
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.lukastomoszek.idea.codemetricsvisualization.db.DuckDbService
import com.lukastomoszek.idea.codemetricsvisualization.db.model.QueryResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.swing.DefaultListModel
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.ListSelectionModel

class SelectTablesDialog(
    project: Project,
    private val initiallySelectedTables: List<String>
) : DialogWrapper(project) {

    private lateinit var tablesCheckBoxList: CheckBoxList<JCheckBox>
    private val checkBoxListModel = DefaultListModel<JCheckBox>()

    init {
        title = "Select Relevant Tables"
        init()
        populateTableNames(project)
    }

    override fun createCenterPanel(): JComponent {
        tablesCheckBoxList = CheckBoxList<JCheckBox>().apply {
            model = checkBoxListModel
            selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
            visibleRowCount = 10
            emptyText.text = "No tables found in database"
        }

        return panel {
            row {
                cell(JBScrollPane(tablesCheckBoxList))
                    .align(Align.FILL)
            }.resizableRow()
        }
    }

    private fun populateTableNames(project: Project) {
        val duckDbService = DuckDbService.getInstance(project)
        val result: Result<QueryResult> = runBlocking(Dispatchers.IO) {
            duckDbService.executeReadQuery("SHOW TABLES;")
        }

        checkBoxListModel.clear()
        val currentDbTables = result.getOrNull()?.rows?.mapNotNull { it["name"] as? String }?.sorted() ?: emptyList()

        currentDbTables.forEach { tableName ->
            val checkBox = JBCheckBox(tableName)
            checkBox.isSelected = initiallySelectedTables.contains(tableName)
            checkBoxListModel.addElement(checkBox)
        }
    }

    fun getSelectedTableNames(): List<String> {
        val selectedTables = mutableListOf<String>()
        for (i in 0 until checkBoxListModel.size()) {
            val checkBox = checkBoxListModel.getElementAt(i)
            if (checkBox.isSelected) {
                selectedTables.add(checkBox.text)
            }
        }
        return selectedTables
    }
}
