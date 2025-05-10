package com.lukastomoszek.idea.codemetricsvisualization.config.persistence

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DataSourceSettingsState

@Service(Service.Level.PROJECT)
@State(
    name = "DataSourceSettings",
    storages = [Storage("codeMetricsVisualizations/dataSourceSettings.xml")]
)
class DataSourceSettings : AbstractSettingsStateComponent<DataSourceSettingsState>() {

    override var internalState = DataSourceSettingsState()

    companion object {
        fun getInstance(project: Project): DataSourceSettings =
            project.getService(DataSourceSettings::class.java)
    }
}
