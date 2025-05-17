package com.lukastomoszek.idea.codemetricsvisualization.config.state

import com.intellij.util.xmlb.annotations.XCollection
import com.lukastomoszek.idea.codemetricsvisualization.linemarker.rule.RangeRule

internal object DefaultLineMarkerConfig {
    const val ENABLED = true
    const val NAME = "New Line Marker Config"
    const val SQL_TEMPLATE = "SELECT COUNT(*) FROM log_entries WHERE method_fqn = '#method_fqn#'"
    val RULES = mutableListOf(
        RangeRule(fromString = "50", toString = "", colorHex = "#FF0000"),
        RangeRule(fromString = "10", toString = "50", colorHex = "#FFFF00"),
        RangeRule(fromString = "0", toString = "10", colorHex = "#00FF00")
    )
    const val LLM_DESCRIPTION = ""
    val LLM_RELEVANT_TABLE_NAMES: List<String> = emptyList()
}

data class LineMarkerConfig(
    var enabled: Boolean = DefaultLineMarkerConfig.ENABLED,
    override var name: String = DefaultLineMarkerConfig.NAME,
    var sqlTemplate: String = DefaultLineMarkerConfig.SQL_TEMPLATE,
    @XCollection
    var lineMarkerRules: MutableList<RangeRule> = DefaultLineMarkerConfig.RULES.map { it.copy() }.toMutableList(),
    var llmDescription: String = DefaultLineMarkerConfig.LLM_DESCRIPTION,
    @XCollection
    var llmRelevantTableNames: List<String> = DefaultLineMarkerConfig.LLM_RELEVANT_TABLE_NAMES
) : NamedConfig

data class LineMarkerSettingsState(
    @XCollection
    val configs: List<LineMarkerConfig> = listOf()
)
