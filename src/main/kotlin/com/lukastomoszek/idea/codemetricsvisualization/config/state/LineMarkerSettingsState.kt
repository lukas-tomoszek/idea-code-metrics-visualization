/*
 * Copyright (c) 2025 Lukáš Tomoszek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lukastomoszek.idea.codemetricsvisualization.config.state

import com.intellij.util.xmlb.annotations.XCollection
import com.lukastomoszek.idea.codemetricsvisualization.linemarker.rule.RangeRule

internal object DefaultLineMarkerConfig {
    const val ENABLED = true
    const val NAME = "New Line Marker Config"
    const val SQL_TEMPLATE = "SELECT COUNT(*) FROM logs WHERE method_fqn = '#method_fqn#'"
    val RULES = mutableListOf(
        RangeRule(fromString = "50", toString = "", colorHex = "#FF0000"),
        RangeRule(fromString = "10", toString = "50", colorHex = "#FFFF00"),
        RangeRule(fromString = "0", toString = "10", colorHex = "#00FF00")
    )
    const val LLM_DESCRIPTION = ""
    val LLM_RELEVANT_TABLE_NAMES: List<String> = emptyList()
}

data class LineMarkerConfig(
    @Suppress("KotlinConstantConditions")
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
