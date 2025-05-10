package com.lukastomoszek.idea.codemetricsvisualization.db.model

data class QueryResult(
    val columnNames: List<String>,
    val columnTypes: List<String>,
    val rows: List<Map<String, Any?>>
)
