package com.lukastomoszek.idea.codemetricsvisualization.config.persistence

import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ChartConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ChartSettingsState

@Service(Service.Level.PROJECT)
@State(
    name = "ChartSettings",
    storages = [Storage("codeMetricsVisualizations/chartSettings.xml")]
)
class ChartSettings : SerializablePersistentStateComponent<ChartSettingsState>(ChartSettingsState()) {

    fun update(newConfigs: List<ChartConfig>) {
        updateState { it.copy(configs = newConfigs) }
    }

    companion object {
        fun getInstance(project: Project): ChartSettings =
            project.getService(ChartSettings::class.java)
    }
}
