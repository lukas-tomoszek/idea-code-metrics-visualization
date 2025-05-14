package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.model

import com.lukastomoszek.idea.codemetricsvisualization.db.model.QueryResult

data class ChartResponse(
    val queryResult: QueryResult? = null,
    val errorMessage: String? = null,
    val originalRequest: ChartRequest
)
