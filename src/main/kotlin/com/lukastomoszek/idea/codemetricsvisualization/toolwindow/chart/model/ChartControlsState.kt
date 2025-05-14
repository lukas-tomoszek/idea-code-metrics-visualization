package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.model

import com.lukastomoszek.idea.codemetricsvisualization.config.state.ChartConfig
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.ui.ChartControlsProvider

data class ChartControlsState(
    val currentChartConfig: ChartConfig? = null,
    val currentMethodFilter: String? = ChartControlsProvider.ALL_METHODS_OPTION,
    val currentFeatureFilter: String? = ChartControlsProvider.ALL_FEATURES_OPTION
)
