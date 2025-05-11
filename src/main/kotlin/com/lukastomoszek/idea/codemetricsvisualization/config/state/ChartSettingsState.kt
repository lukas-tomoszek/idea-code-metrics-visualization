package com.lukastomoszek.idea.codemetricsvisualization.config.state

import com.intellij.util.xmlb.annotations.XCollection

object DefaultChartConfig {
    const val NAME = "New Chart"
    val SQL: String = """
        SELECT 'CategoryA' AS label, 10 AS value
        UNION ALL SELECT 'CategoryB' AS label, 20 AS value
        UNION ALL SELECT 'CategoryC' AS label, 15 AS value;
    """.trimIndent()
}

data class ChartSettingsState(
    @XCollection
    var charts: MutableList<ChartConfig> = mutableListOf()
)

data class ChartConfig(
    var name: String = DefaultChartConfig.NAME,
    var sql: String = DefaultChartConfig.SQL
)
