package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ChartConfig
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder

class ChartUIManager(
    private val controlsProvider: ChartControlsProvider,
    private val chartConfigsModel: CollectionComboBoxModel<ChartConfig>,
    private val methodFilterModel: CollectionComboBoxModel<String>,
    private val featureFilterModel: CollectionComboBoxModel<String>
) {
    private var isProgrammaticChange = false

    fun updateChartConfigComboBox(configs: List<ChartConfig>, currentConfig: ChartConfig?) {
        try {
            isProgrammaticChange = true
            val oldSelection = controlsProvider.chartConfigComboBox.selectedItem as? ChartConfig
            if (chartConfigsModel.items != configs || oldSelection?.name != currentConfig?.name || (oldSelection != null && currentConfig != null && oldSelection.sqlTemplate != currentConfig.sqlTemplate)) {
                chartConfigsModel.removeAll()
                chartConfigsModel.add(configs)
                controlsProvider.chartConfigComboBox.selectedItem = currentConfig
            }
        } finally {
            isProgrammaticChange = false
        }
    }

    private fun updateStringFilterModel(
        model: CollectionComboBoxModel<String>,
        comboBox: ComboBox<String>,
        newItems: List<String>,
        currentFilterFromState: String?,
        allOption: String
    ): String? {
        var effectiveFilter = currentFilterFromState
        if (model.items != newItems) {
            try {
                isProgrammaticChange = true
                val oldSelection = comboBox.selectedItem as? String
                model.removeAll()
                model.add(newItems)

                effectiveFilter = if (newItems.contains(oldSelection)) {
                    oldSelection
                } else if (newItems.contains(currentFilterFromState)) {
                    currentFilterFromState
                } else {
                    allOption
                }
                comboBox.selectedItem = effectiveFilter
            } finally {
                isProgrammaticChange = false
            }
        } else if (comboBox.selectedItem != currentFilterFromState) {
            try {
                isProgrammaticChange = true
                comboBox.selectedItem = currentFilterFromState
            } finally {
                isProgrammaticChange = false
            }
        }
        return effectiveFilter
    }

    fun updateMethodFilterModel(methodsInFile: List<String>, currentFilterFromState: String?): String? {
        return updateStringFilterModel(
            methodFilterModel,
            controlsProvider.methodFilterComboBox,
            methodsInFile,
            currentFilterFromState,
            ChartControlsProvider.ALL_METHODS_OPTION
        )
    }

    fun updateFeatureFilterModel(featuresInFile: List<String>, currentFilterFromState: String?): String? {
        return updateStringFilterModel(
            featureFilterModel,
            controlsProvider.featureFilterComboBox,
            featuresInFile,
            currentFilterFromState,
            ChartControlsProvider.ALL_FEATURES_OPTION
        )
    }

    fun updateControlsVisualState(
        currentChartConfig: ChartConfig?,
        isMethodFilterLocked: Boolean,
        isFeatureFilterLocked: Boolean
    ) {
        controlsProvider.chartConfigComboBox.isEnabled = chartConfigsModel.size > 0

        val methodFilteringApplicable =
            currentChartConfig?.sqlTemplate?.contains(ContextAwareQueryBuilder.METHOD_FQN_PLACEHOLDER) ?: false
        controlsProvider.methodFilterComboBox.isEnabled = methodFilteringApplicable && methodFilterModel.size > 0
        controlsProvider.lockMethodButton.isEnabled = methodFilteringApplicable
        controlsProvider.updateLockButton(
            controlsProvider.lockMethodButton,
            isMethodFilterLocked && methodFilteringApplicable
        )

        val featureFilteringApplicable =
            currentChartConfig?.sqlTemplate?.contains(ContextAwareQueryBuilder.FEATURE_NAME_PLACEHOLDER) ?: false
        controlsProvider.featureFilterComboBox.isEnabled = featureFilteringApplicable && featureFilterModel.size > 0
        controlsProvider.lockFeatureButton.isEnabled = featureFilteringApplicable
        controlsProvider.updateLockButton(
            controlsProvider.lockFeatureButton,
            isFeatureFilterLocked && featureFilteringApplicable
        )
    }

    fun synchronizeComboBoxSelections(
        currentChartConfig: ChartConfig?,
        currentMethodFilter: String?,
        currentFeatureFilter: String?
    ) {
        try {
            isProgrammaticChange = true
            if (controlsProvider.chartConfigComboBox.selectedItem != currentChartConfig) {
                controlsProvider.chartConfigComboBox.selectedItem = currentChartConfig
            }
            if (controlsProvider.methodFilterComboBox.selectedItem != currentMethodFilter) {
                controlsProvider.methodFilterComboBox.selectedItem = currentMethodFilter
            }
            if (controlsProvider.featureFilterComboBox.selectedItem != currentFeatureFilter) {
                controlsProvider.featureFilterComboBox.selectedItem = currentFeatureFilter
            }
        } finally {
            isProgrammaticChange = false
        }
    }
}
