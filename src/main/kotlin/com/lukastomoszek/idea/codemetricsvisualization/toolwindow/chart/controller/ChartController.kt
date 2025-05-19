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

package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.controller

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.CollectionComboBoxModel
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ChartConfig
import com.lukastomoszek.idea.codemetricsvisualization.context.model.ContextInfo
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.listener.ChartContextListener
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.model.ChartRequest
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.service.ChartService
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.ui.ChartControlsProvider
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.ui.ChartViewerPanel

class ChartController(
    private val project: Project,
    private val chartViewerPanel: ChartViewerPanel
) : Disposable {

    private val chartContextListener = ChartContextListener(project, ::handleContextUpdate)

    internal val controlsProvider: ChartControlsProvider

    private val chartConfigsModel = CollectionComboBoxModel<ChartConfig>()
    private val methodFilterModel = CollectionComboBoxModel<String>()
    private val featureFilterModel = CollectionComboBoxModel<String>()
    private val mappingPathFilterModel = CollectionComboBoxModel<String>()
    private val mappingMethodFilterModel = CollectionComboBoxModel<String>()

    init {
        controlsProvider = ChartControlsProvider(
            project,
            chartConfigsModel,
            methodFilterModel,
            featureFilterModel,
            mappingPathFilterModel,
            mappingMethodFilterModel,
            onChartConfigSelected = {
                resetFiltersBasedOnConfig()
                updateContext()
            },
            onMethodFilterSelected = { fetchAndDisplayChartData() },
            onFeatureFilterSelected = { fetchAndDisplayChartData() },
            onMappingPathFilterSelected = { fetchAndDisplayChartData() },
            onMappingMethodFilterSelected = { fetchAndDisplayChartData() },
            onChartDropdownOpening = { loadChartConfigurations() }
        )
        Disposer.register(this, chartContextListener)
    }

    fun initialize() {
        chartContextListener.register()
        loadChartConfigurations()
        resetFiltersBasedOnConfig()
        updateContext()
    }

    fun loadChartConfigurations() {
        val previousSelectedConfig = controlsProvider.getSelectedChartConfig()
        val configs = ChartService.getInstance(project).getAvailableChartConfigs()
        controlsProvider.updateChartConfigComboBox(previousSelectedConfig, configs)
    }

    private fun isMethodFilterApplicable(): Boolean {
        return controlsProvider.getSelectedChartConfig()?.sqlTemplate
                   ?.contains(ContextAwareQueryBuilder.METHOD_FQN_PLACEHOLDER) ?: false
    }

    private fun isFeatureFilterApplicable(): Boolean {
        return controlsProvider.getSelectedChartConfig()?.sqlTemplate
                   ?.contains(ContextAwareQueryBuilder.FEATURE_NAME_PLACEHOLDER) ?: false
    }

    private fun isMappingPathFilterApplicable(): Boolean {
        return controlsProvider.getSelectedChartConfig()?.sqlTemplate
                   ?.contains(ContextAwareQueryBuilder.MAPPING_PATH_PLACEHOLDER) ?: false
    }

    private fun isMappingMethodFilterApplicable(): Boolean {
        return controlsProvider.getSelectedChartConfig()?.sqlTemplate
                   ?.contains(ContextAwareQueryBuilder.MAPPING_METHOD_PLACEHOLDER) ?: false
    }

    private fun resetFiltersBasedOnConfig() {
        val methodEnabled = isMethodFilterApplicable()
        val featureEnabled = isFeatureFilterApplicable()
        val mappingPathEnabled = isMappingPathFilterApplicable()
        val mappingMethodEnabled = isMappingMethodFilterApplicable()

        controlsProvider.methodFilterComboBox.isEnabled = methodEnabled
        controlsProvider.featureFilterComboBox.isEnabled = featureEnabled
        controlsProvider.mappingPathFilterComboBox.isEnabled = mappingPathEnabled
        controlsProvider.mappingMethodFilterComboBox.isEnabled = mappingMethodEnabled

        if (!methodEnabled) {
            controlsProvider.updateMethodFilterModel(ChartControlsProvider.ALL_METHODS_OPTION)
        }
        if (!featureEnabled) {
            controlsProvider.updateFeatureFilterModel(ChartControlsProvider.ALL_FEATURES_OPTION)
        }
        if (!mappingPathEnabled) {
            controlsProvider.updateMappingPathFilterModel(ChartControlsProvider.ALL_MAPPING_PATHS_OPTION)
        }
        if (!mappingMethodEnabled) {
            controlsProvider.updateMappingMethodFilterModel(ChartControlsProvider.ALL_MAPPING_METHODS_OPTION)
        }
    }

    private fun updateContext() {
        ChartService.getInstance(project).scheduleContextUpdate { context -> handleContextUpdate(context) }
    }

    private fun handleContextUpdate(editorContext: ContextInfo) {
        if (!controlsProvider.isContextUpdateLocked()) {
            var ctx = editorContext

            if (!isMethodFilterApplicable()) {
                ctx = ctx.copy(methodFqn = null)
            }
            if (!isFeatureFilterApplicable()) {
                ctx = ctx.copy(featureName = null)
            }
            if (!isMappingPathFilterApplicable()) {
                ctx = ctx.copy(mappingPath = null)
            }
            if (!isMappingMethodFilterApplicable()) {
                ctx = ctx.copy(mappingMethod = null)
            }

            controlsProvider.updateMethodFilterModel(ctx.methodFqn, ctx.allMethodsInFile)
            controlsProvider.updateFeatureFilterModel(ctx.featureName, ctx.allFeaturesInFile)
            controlsProvider.updateMappingPathFilterModel(ctx.mappingPath, ctx.allMappingPathsInFile)
            controlsProvider.updateMappingMethodFilterModel(ctx.mappingMethod, ctx.allMappingMethodsInFile)
            fetchAndDisplayChartData()
        }
    }

    private fun fetchAndDisplayChartData() {
        val config = controlsProvider.getSelectedChartConfig()
        if (config == null) {
            chartViewerPanel.clearChartPanel()
            chartViewerPanel.setStatus(if (chartConfigsModel.isEmpty) "No charts configured. Please add one in settings." else "Select a chart to display.")
            return
        }

        var status = "Loading chart '${config.name}'"
        val selectedMethodFilter = controlsProvider.getSelectedMethodFilter()
        val selectedFeatureFilter = controlsProvider.getSelectedFeatureFilter()
        val selectedMappingPathFilter = controlsProvider.getSelectedMappingPathFilter()
        val selectedMappingMethodFilter = controlsProvider.getSelectedMappingMethodFilter()

        val methodParamForQuery =
            if (isMethodFilterApplicable()) selectedMethodFilter.takeIf { it != ChartControlsProvider.ALL_METHODS_OPTION } else null
        val featureParamForQuery =
            if (isFeatureFilterApplicable()) selectedFeatureFilter.takeIf { it != ChartControlsProvider.ALL_FEATURES_OPTION } else null
        val mappingPathParamForQuery =
            if (isMappingPathFilterApplicable()) selectedMappingPathFilter.takeIf { it != ChartControlsProvider.ALL_MAPPING_PATHS_OPTION } else null
        val mappingMethodParamForQuery =
            if (isMappingMethodFilterApplicable()) selectedMappingMethodFilter.takeIf { it != ChartControlsProvider.ALL_MAPPING_METHODS_OPTION } else null


        if (methodParamForQuery != null) status += " for method '$methodParamForQuery'"
        if (featureParamForQuery != null) status += " for feature '$featureParamForQuery'"
        if (mappingPathParamForQuery != null) status += " for path '$mappingPathParamForQuery'"
        if (mappingMethodParamForQuery != null) status += " (HTTP ${mappingMethodParamForQuery})"

        chartViewerPanel.setStatus("$status...")
        chartViewerPanel.clearChartPanel()

        val requestContextInfo = ContextInfo(
            methodFqn = methodParamForQuery,
            featureName = featureParamForQuery,
            allMethodsInFile = controlsProvider.getMethodsFqnsInFile(),
            allFeaturesInFile = controlsProvider.getFeatureNamesInFile(),
            mappingPath = mappingPathParamForQuery,
            mappingMethod = mappingMethodParamForQuery,
            allMappingPathsInFile = controlsProvider.getMappingPathsInFile(),
            allMappingMethodsInFile = controlsProvider.getMappingMethodsInFile(),
        )
        val request = ChartRequest(config, requestContextInfo)

        ChartService.getInstance(project).fetchChartData(request) { response ->
            if (response.errorMessage != null) {
                chartViewerPanel.setStatus("Error loading chart '${config.name}': ${response.errorMessage}")
            } else if (response.queryResult != null) {
                if (response.queryResult.rows.isEmpty()) {
                    chartViewerPanel.setStatus("No data found for chart '${config.name}' with current filters.")
                } else {
                    chartViewerPanel.updateChart(response.queryResult, config.name)
                }
            } else {
                chartViewerPanel.setStatus("Failed to load chart data for '${config.name}'.")
            }
        }
    }

    override fun dispose() {}
}
