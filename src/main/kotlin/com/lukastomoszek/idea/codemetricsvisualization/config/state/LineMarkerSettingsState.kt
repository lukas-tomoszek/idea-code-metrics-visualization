package com.lukastomoszek.idea.codemetricsvisualization.config.state

import com.intellij.util.xmlb.annotations.XCollection
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder

internal object DefaultLineMarkerConfig {
    const val ENABLED = true
    const val NAME = "New Line Marker Config"
    const val SQL_TEMPLATE = "SELECT COUNT(*) FROM log_entries WHERE method_fqn = '#method_fqn#'"
    val RULES = mutableListOf(
        LineMarkerRule(LineMarkerOperator.GREATER_THAN, 50.0f, "#FF0000"),
        LineMarkerRule(LineMarkerOperator.GREATER_THAN, 10.0f, "#FFFF00"),
        LineMarkerRule(LineMarkerOperator.GREATER_THAN, 0.0f, "#00FF00")
    )
    const val LLM_DESCRIPTION = ""
    val LLM_RELEVANT_TABLE_NAMES: List<String> = emptyList()
}

data class LineMarkerConfig(
    var enabled: Boolean = DefaultLineMarkerConfig.ENABLED,
    override var name: String = DefaultLineMarkerConfig.NAME,
    var sqlTemplate: String = DefaultLineMarkerConfig.SQL_TEMPLATE,
    @XCollection
    var lineMarkerRules: MutableList<LineMarkerRule> = DefaultLineMarkerConfig.RULES.map { it.copy() }.toMutableList(),
    var llmDescription: String = DefaultLineMarkerConfig.LLM_DESCRIPTION,
    @XCollection
    var llmRelevantTableNames: List<String> = DefaultLineMarkerConfig.LLM_RELEVANT_TABLE_NAMES
) : NamedConfig {
    fun hasMethodFqnPlaceholder(): Boolean {
        return sqlTemplate.contains(ContextAwareQueryBuilder.METHOD_FQN_PLACEHOLDER)
    }

    fun hasFeatureNamePlaceholder(): Boolean {
        return sqlTemplate.contains(ContextAwareQueryBuilder.FEATURE_NAME_PLACEHOLDER)
    }
}

data class LineMarkerSettingsState(
    @XCollection
    val configs: List<LineMarkerConfig> = listOf()
)
