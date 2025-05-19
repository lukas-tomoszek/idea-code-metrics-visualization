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

package com.lukastomoszek.idea.codemetricsvisualization.config.persistence

import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerSettingsState

@Service(Service.Level.PROJECT)
@State(
    name = "LineMarkerSettings",
    storages = [Storage("codeMetricsVisualizations/lineMarkerSettings.xml")]
)
class LineMarkerSettings : SerializablePersistentStateComponent<LineMarkerSettingsState>(LineMarkerSettingsState()) {

    fun update(newConfigs: List<LineMarkerConfig>) {
        updateState { it.copy(configs = newConfigs) }
    }

    fun getEnabledNonEmptyConfigs(): List<LineMarkerConfig> =
        state.configs.filter {
            it.enabled && it.sqlTemplate.isNotBlank() && it.lineMarkerRules.isNotEmpty()
        }

    companion object {
        fun getInstance(project: Project): LineMarkerSettings =
            project.getService(LineMarkerSettings::class.java)
    }
}
