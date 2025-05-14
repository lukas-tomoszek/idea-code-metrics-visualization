package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.model

import com.lukastomoszek.idea.codemetricsvisualization.context.model.ContextInfo

data class EditorChartContext(
    val focusedContext: ContextInfo?,
    val allMethodsInFile: List<String>,
    val allFeaturesInFile: List<String>
)
