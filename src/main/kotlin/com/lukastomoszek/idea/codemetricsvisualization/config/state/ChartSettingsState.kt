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

object DefaultChartConfig {
    const val NAME = "New Chart"
    val SQL_TEMPLATE: String = """
        SELECT 
            date_trunc('day', timestamp) AS day, 
            COUNT(*) AS call_count
        FROM logs
        WHERE method_fqn LIKE '#method_fqn#'
        GROUP BY day
        ORDER BY day;
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
