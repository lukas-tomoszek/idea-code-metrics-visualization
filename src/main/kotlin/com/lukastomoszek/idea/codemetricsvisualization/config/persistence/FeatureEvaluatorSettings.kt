package com.lukastomoszek.idea.codemetricsvisualization.config.persistence

import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.lukastomoszek.idea.codemetricsvisualization.config.state.FeatureEvaluatorConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.FeatureEvaluatorSettingsState

@Service(Service.Level.PROJECT)
@State(
    name = "FeatureEvaluatorSettings",
    storages = [Storage("codeMetricsVisualizations/featureEvaluatorSettings.xml")]
)
class FeatureEvaluatorSettings :
    SerializablePersistentStateComponent<FeatureEvaluatorSettingsState>(FeatureEvaluatorSettingsState()) {

    fun update(newConfigs: List<FeatureEvaluatorConfig>) {
        updateState { it.copy(featureEvaluators = newConfigs) }
    }

    companion object {
        fun getInstance(project: Project): FeatureEvaluatorSettings =
            project.getService(FeatureEvaluatorSettings::class.java)
    }
}
