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
        updateState { it.copy(lineMarkerConfigs = newConfigs) }
    }

    fun getEnabledLineMarkerConfigs(): List<LineMarkerConfig> =
        state.lineMarkerConfigs.filter {
            it.enabled && it.sqlTemplate.isNotBlank() && it.lineMarkerRules.isNotEmpty()
        }

    companion object {
        fun getInstance(project: Project): LineMarkerSettings =
            project.getService(LineMarkerSettings::class.java)
    }
}
