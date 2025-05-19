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

package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.dbviewer.ui

import javax.swing.table.AbstractTableModel

class DbViewerTableModel : AbstractTableModel() {

    private var columnNames: Array<String> = emptyArray()
    private var columnTypes: Array<String> = emptyArray()
    private var data: List<Array<Any?>> = emptyList()

    fun setData(newColumnNames: Array<String>, newColumnTypes: Array<String>, newData: List<Array<Any?>>) {
        columnNames = newColumnNames
        columnTypes = newColumnTypes
        data = newData
        fireTableStructureChanged()
    }

    fun clearData() {
        columnNames = emptyArray()
        columnTypes = emptyArray()
        data = emptyList()
        fireTableStructureChanged()
    }

    override fun getRowCount(): Int = data.size

    override fun getColumnCount(): Int = columnNames.size

    override fun getColumnName(column: Int): String {
        val name = columnNames.getOrElse(column) { "?" }
        val type = columnTypes.getOrNull(column)?.takeIf { it.isNotBlank() }?.uppercase()
        return if (type != null) "$name ($type)" else name
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        return data.getOrNull(rowIndex)?.getOrNull(columnIndex)
    }

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return Object::class.java
    }
}
