package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.model

import com.lukastomoszek.idea.codemetricsvisualization.config.state.ChartConfig
import com.lukastomoszek.idea.codemetricsvisualization.context.model.ContextInfo

data class ChartRequest(
    val config: ChartConfig,
    val contextInfo: ContextInfo,
)
