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

    private val llmDescriptionColumn = object : ColumnInfo<ChartConfig, String>("LLM Description") {
        override fun valueOf(item: ChartConfig): String = item.llmDescription
    }

    private val llmTablesColumn = object : ColumnInfo<ChartConfig, String>("LLM Tables") {
        override fun valueOf(item: ChartConfig): String = item.llmRelevantTableNames.joinToString(", ")
    }

    private val sqlColumn = object : ColumnInfo<ChartConfig, String>("SQL Template") {
        override fun valueOf(item: ChartConfig): String = item.sqlTemplate
            .replace(Regex("\\s+"), " ")
    }

    override fun getColumnInfos(): Array<ColumnInfo<ChartConfig, *>> =
        arrayOf(nameColumn, llmDescriptionColumn, llmTablesColumn, sqlColumn)

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

    override fun copyItem(item: ChartConfig): ChartConfig = item.copy(
        llmRelevantTableNames = item.llmRelevantTableNames.toList()
    )
}
