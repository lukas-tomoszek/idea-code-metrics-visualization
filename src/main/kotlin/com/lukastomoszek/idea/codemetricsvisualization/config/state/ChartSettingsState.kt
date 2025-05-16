package com.lukastomoszek.idea.codemetricsvisualization.config.state

import com.intellij.util.xmlb.annotations.XCollection

object DefaultChartConfig {
    const val NAME = "New Chart"
    val SQL_TEMPLATE: String = """
        SELECT 'CategoryA' AS label, 10 AS value
        UNION ALL SELECT 'CategoryB' AS label, 20 AS value
        UNION ALL SELECT 'CategoryC' AS label, 15 AS value;
    """.trimIndent()
    const val LLM_DESCRIPTION = ""
    val LLM_RELEVANT_TABLE_NAMES: List<String> = emptyList()
}

data class ChartConfig(
    override var name: String = DefaultChartConfig.NAME,
    var sqlTemplate: String = DefaultChartConfig.SQL_TEMPLATE,
    var llmDescription: String = DefaultChartConfig.LLM_DESCRIPTION,
    @XCollection
    var llmRelevantTableNames: List<String> = DefaultChartConfig.LLM_RELEVANT_TABLE_NAMES
) : NamedConfig

data class ChartSettingsState(
    @XCollection
    val configs: List<ChartConfig> = listOf()
)
