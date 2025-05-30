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

package com.lukastomoszek.idea.codemetricsvisualization.config.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.BooleanTableCellEditor
import com.intellij.ui.BooleanTableCellRenderer
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ColumnInfo
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.LineMarkerSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog.AbstractNamedDialog
import com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog.LineMarkerDialog
import javax.swing.table.TableCellRenderer

class LineMarkerConfigurable(project: Project) :
    AbstractListNamedConfigurable<LineMarkerConfig>(
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

    private val llmDescriptionColumn = object : ColumnInfo<LineMarkerConfig, String>("LLM Description") {
        override fun valueOf(item: LineMarkerConfig): String = item.llmDescription
    }

    private val llmTablesColumn = object : ColumnInfo<LineMarkerConfig, String>("LLM Tables") {
        override fun valueOf(item: LineMarkerConfig): String = item.llmRelevantTableNames.joinToString(", ")
    }

    private val sqlTemplateColumn = object : ColumnInfo<LineMarkerConfig, String>("SQL Template") {
        override fun valueOf(item: LineMarkerConfig): String = item.sqlTemplate
            .replace(Regex("\\s+"), " ")
    }

    private val rulesCountColumn = object : ColumnInfo<LineMarkerConfig, String>("Rules") {
        override fun valueOf(item: LineMarkerConfig): String = "${item.lineMarkerRules.size} rules"
    }

    override fun getColumnInfos(): Array<ColumnInfo<LineMarkerConfig, *>> =
        arrayOf(enabledColumn, nameColumn, llmDescriptionColumn, llmTablesColumn, sqlTemplateColumn, rulesCountColumn)

    override fun updateTable(table: JBTable) {
        val enabledCol = table.columnModel.getColumn(0)
        enabledCol.cellRenderer = BooleanTableCellRenderer()
        enabledCol.cellEditor = BooleanTableCellEditor()

        val preferredWidth = 70
        enabledCol.preferredWidth = preferredWidth
        enabledCol.minWidth = preferredWidth
        enabledCol.maxWidth = preferredWidth
    }

    override fun createNewItem(): LineMarkerConfig = LineMarkerConfig()

    override fun createEditDialog(item: LineMarkerConfig): AbstractNamedDialog<LineMarkerConfig> {
        val otherNames = items.filterNot { it === item }.map { it.name }
        return LineMarkerDialog(project, item.copy(), otherNames)
    }

    override fun getItemsFromSettings(): List<LineMarkerConfig> =
        LineMarkerSettings.getInstance(project).state.configs

    override fun saveItemsToSettings(items: List<LineMarkerConfig>) {
        LineMarkerSettings.getInstance(project).update(items)
    }

    override fun copyItem(item: LineMarkerConfig): LineMarkerConfig =
        item.copy(
            lineMarkerRules = item.lineMarkerRules.map { it.copy() }.toMutableList(),
            llmRelevantTableNames = item.llmRelevantTableNames.toList()
        )
}
