package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.controller

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.CollectionComboBoxModel
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ChartConfig
import com.lukastomoszek.idea.codemetricsvisualization.context.model.ContextInfo
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.listener.ChartContextListener
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.model.ChartControlsState
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.model.ChartRequest
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.model.EditorChartContext
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.model.FilterLockManager
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.service.ChartService
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.ui.ChartControlsProvider
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.ui.ChartUIManager
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.ui.ChartViewerPanel
import kotlinx.coroutines.*

class ChartController(
    private val project: Project,
    private val chartViewerPanel: ChartViewerPanel
) : Disposable {

    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val chartService = ChartService.getInstance(project)
    private val chartContextListener = ChartContextListener(project, controllerScope, ::handleContextUpdate)
    private val filterLockManager = FilterLockManager()
    private val chartUIManager: ChartUIManager

    internal val controlsProvider: ChartControlsProvider

    private val chartConfigsModel = CollectionComboBoxModel<ChartConfig>()
    private val methodFilterModel = CollectionComboBoxModel<String>()
    private val featureFilterModel = CollectionComboBoxModel<String>()

    private var controlsState = ChartControlsState()
    private var lastEditorChartContext: EditorChartContext? = null

    init {
        controlsProvider = ChartControlsProvider(
            project,
            chartConfigsModel,
            methodFilterModel,
            featureFilterModel,
            onChartConfigSelected = { config ->
                controlsState = controlsState.copy(currentChartConfig = config)
                resetFiltersBasedOnConfig()
                refreshUIAndFetchData()
            },
            onMethodFilterSelected = { method ->
                controlsState = controlsState.copy(currentMethodFilter = method)
                refreshUIAndFetchData()
            },
            onFeatureFilterSelected = { feature ->
                controlsState = controlsState.copy(currentFeatureFilter = feature)
                refreshUIAndFetchData()
            },
            onChartDropdownOpening = { loadChartConfigurations() },
            onMethodLockToggled = {
                filterLockManager.toggleMethodLock()
                refreshUIAndFetchData()
            },
            onFeatureLockToggled = {
                filterLockManager.toggleFeatureLock()
                refreshUIAndFetchData()
            },
            isMethodFilterEnabled = { isMethodFilterApplicable() },
            isFeatureFilterEnabled = { isFeatureFilterApplicable() }
        )
        chartUIManager = ChartUIManager(controlsProvider, chartConfigsModel, methodFilterModel, featureFilterModel)
        Disposer.register(this, chartContextListener)
    }

    fun initialize() {
        chartContextListener.register()
        loadChartConfigurations()
    }

    fun loadChartConfigurations() {
        val previousSelectedName = controlsState.currentChartConfig?.name
        val configs = chartService.getAvailableChartConfigs()
        val newChartConfig = if (previousSelectedName != null) {
            configs.find { it.name == previousSelectedName } ?: configs.firstOrNull()
        } else {
            configs.firstOrNull()
        }
        controlsState = controlsState.copy(currentChartConfig = newChartConfig)
        chartUIManager.updateChartConfigComboBox(configs, controlsState.currentChartConfig)
        resetFiltersBasedOnConfig()
        refreshUIAndFetchData()
    }

    private fun isMethodFilterApplicable(): Boolean {
        return controlsState.currentChartConfig?.sqlTemplate?.contains(ContextAwareQueryBuilder.METHOD_NAME_PLACEHOLDER)
            ?: false
    }

    private fun isFeatureFilterApplicable(): Boolean {
        return controlsState.currentChartConfig?.sqlTemplate?.contains(ContextAwareQueryBuilder.FEATURE_NAME_PLACEHOLDER)
            ?: false
    }

    private fun resetFiltersBasedOnConfig() {
        var newMethodFilter = controlsState.currentMethodFilter
        var newFeatureFilter = controlsState.currentFeatureFilter

        if (!isMethodFilterApplicable()) {
            newMethodFilter = ChartControlsProvider.ALL_METHODS_OPTION
            filterLockManager.setMethodLock(false)
        }
        if (!isFeatureFilterApplicable()) {
            newFeatureFilter = ChartControlsProvider.ALL_FEATURES_OPTION
            filterLockManager.setFeatureLock(false)
        }
        controlsState = controlsState.copy(
            currentMethodFilter = newMethodFilter,
            currentFeatureFilter = newFeatureFilter
        )
    }

    private fun handleContextUpdate(editorChartContext: EditorChartContext?) {
        lastEditorChartContext = editorChartContext

        var newMethodFilter = controlsState.currentMethodFilter
        var newFeatureFilter = controlsState.currentFeatureFilter

        val context = lastEditorChartContext
        val methodsInFile =
            listOf(ChartControlsProvider.ALL_METHODS_OPTION) + (context?.allMethodsInFile ?: emptyList())
        val featuresInFile =
            listOf(ChartControlsProvider.ALL_FEATURES_OPTION) + (context?.allFeaturesInFile ?: emptyList())

        if (isMethodFilterApplicable() && !filterLockManager.isMethodFilterLocked) {
            val focusedMethod = context?.focusedContext?.methodFqn ?: ChartControlsProvider.ALL_METHODS_OPTION
            newMethodFilter =
                if (methodsInFile.contains(focusedMethod)) focusedMethod else ChartControlsProvider.ALL_METHODS_OPTION
        }
        if (isFeatureFilterApplicable() && !filterLockManager.isFeatureFilterLocked) {
            val focusedFeature = context?.focusedContext?.featureName ?: ChartControlsProvider.ALL_FEATURES_OPTION
            newFeatureFilter =
                if (featuresInFile.contains(focusedFeature)) focusedFeature else ChartControlsProvider.ALL_FEATURES_OPTION
        }
        controlsState =
            controlsState.copy(currentMethodFilter = newMethodFilter, currentFeatureFilter = newFeatureFilter)
        refreshUIAndFetchData()
    }

    private fun refreshUIAndFetchData() {
        controllerScope.launch {
            withContext(Dispatchers.EDT) {
                if (project.isDisposed) return@withContext
                val context = lastEditorChartContext

                if (!filterLockManager.isMethodFilterLocked) {
                    val methodsInFile =
                        listOf(ChartControlsProvider.ALL_METHODS_OPTION) + (context?.allMethodsInFile ?: emptyList())
                    val updatedMethodFilter =
                        chartUIManager.updateMethodFilterModel(methodsInFile, controlsState.currentMethodFilter)
                    controlsState = controlsState.copy(
                        currentMethodFilter = updatedMethodFilter,
                    )
                    chartUIManager.updateControlsVisualState(
                        controlsState.currentChartConfig,
                        filterLockManager.isMethodFilterLocked,
                        filterLockManager.isFeatureFilterLocked
                    )
                }

                if (!filterLockManager.isFeatureFilterLocked) {
                    val featuresInFile =
                        listOf(ChartControlsProvider.ALL_FEATURES_OPTION) + (context?.allFeaturesInFile ?: emptyList())
                    val updatedFeatureFilter =
                        chartUIManager.updateFeatureFilterModel(featuresInFile, controlsState.currentFeatureFilter)
                    controlsState = controlsState.copy(
                        currentFeatureFilter = updatedFeatureFilter
                    )
                    chartUIManager.synchronizeComboBoxSelections(
                        controlsState.currentChartConfig,
                        controlsState.currentMethodFilter,
                        controlsState.currentFeatureFilter
                    )
                }

                fetchAndDisplayChartData()
            }
        }
    }

    private fun fetchAndDisplayChartData() {
        val config = controlsState.currentChartConfig
        if (config == null) {
            chartViewerPanel.clearChartPanel()
            chartViewerPanel.setStatus(if (chartConfigsModel.isEmpty) "No charts configured. Please add one in settings." else "Select a chart to display.")
            return
        }

        var status = "Loading chart '${config.name}'"
        val methodParam =
            if (isMethodFilterApplicable()) controlsState.currentMethodFilter.takeIf { it != ChartControlsProvider.ALL_METHODS_OPTION } else null
        val featureParam =
            if (isFeatureFilterApplicable()) controlsState.currentFeatureFilter.takeIf { it != ChartControlsProvider.ALL_FEATURES_OPTION } else null

        if (methodParam != null) status += " for method '$methodParam'"
        if (featureParam != null) status += " for feature '$featureParam'"
        chartViewerPanel.setStatus("$status...")
        chartViewerPanel.clearChartPanel()

        val requestContext = ContextInfo(methodParam, featureParam)
        val request = ChartRequest(config, requestContext)

        chartService.fetchChartData(request) { response ->
            val configFromResponse = response.originalRequest.config
            val contextFromResponse = response.originalRequest.contextInfo

            val configMatches = configFromResponse == controlsState.currentChartConfig

            val methodFilterRelevantToRequest =
                configFromResponse.sqlTemplate.contains(ContextAwareQueryBuilder.METHOD_NAME_PLACEHOLDER)
            val methodFilterMatches = if (methodFilterRelevantToRequest) {
                (contextFromResponse.methodFqn
                    ?: ChartControlsProvider.ALL_METHODS_OPTION) == controlsState.currentMethodFilter
            } else {
                true
            }

            val featureFilterRelevantToRequest =
                configFromResponse.sqlTemplate.contains(ContextAwareQueryBuilder.FEATURE_NAME_PLACEHOLDER)
            val featureFilterMatches = if (featureFilterRelevantToRequest) {
                (contextFromResponse.featureName
                    ?: ChartControlsProvider.ALL_FEATURES_OPTION) == controlsState.currentFeatureFilter
            } else {
                true
            }
            val requestMatchesCurrentUI: Boolean = configMatches && methodFilterMatches && featureFilterMatches

            if (!requestMatchesCurrentUI) {
                return@fetchChartData
            }

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

    override fun dispose() {
        controllerScope.cancel()
    }
}
