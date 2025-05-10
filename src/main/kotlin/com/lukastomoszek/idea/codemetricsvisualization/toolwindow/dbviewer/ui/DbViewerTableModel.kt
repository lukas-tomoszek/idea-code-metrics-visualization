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
