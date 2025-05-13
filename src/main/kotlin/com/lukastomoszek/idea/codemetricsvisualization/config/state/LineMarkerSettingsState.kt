package com.lukastomoszek.idea.codemetricsvisualization.config.state

import com.intellij.util.xmlb.annotations.XCollection

internal object DefaultLineMarkerConfig {
    const val NAME = "New Line Marker Config"
    const val SQL_TEMPLATE = "SELECT COUNT(*) FROM log_entries WHERE method_name = '#method_name#'"
    val RULES = mutableListOf(
        LineMarkerRule(LineMarkerOperator.GREATER_THAN, 50.0f, "#FF0000"),
        LineMarkerRule(LineMarkerOperator.GREATER_THAN, 10.0f, "#FFFF00"),
        LineMarkerRule(LineMarkerOperator.GREATER_THAN, 0.0f, "#00FF00")
    )
    const val ENABLED = true
}

data class LineMarkerSettingsState(
    @XCollection
    val configs: List<LineMarkerConfig> = listOf()
)

data class LineMarkerConfig(
    var enabled: Boolean = DefaultLineMarkerConfig.ENABLED,
    override var name: String = DefaultLineMarkerConfig.NAME,
    var sqlTemplate: String = DefaultLineMarkerConfig.SQL_TEMPLATE,

    @XCollection
    var lineMarkerRules: MutableList<LineMarkerRule> = DefaultLineMarkerConfig.RULES.map { it.copy() }.toMutableList()
) : NamedConfig
