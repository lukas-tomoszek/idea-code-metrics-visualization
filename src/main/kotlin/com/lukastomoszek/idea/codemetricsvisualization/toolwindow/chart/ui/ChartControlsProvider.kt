package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ChartConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.ui.ChartConfigurable
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

class ChartControlsProvider(
    private val project: Project,
    val chartConfigsModel: CollectionComboBoxModel<ChartConfig>,
    val methodFilterModel: CollectionComboBoxModel<String>,
    val featureFilterModel: CollectionComboBoxModel<String>,
    val mappingPathFilterModel: CollectionComboBoxModel<String>,
    val mappingMethodFilterModel: CollectionComboBoxModel<String>,
    val onChartConfigSelected: () -> Unit,
    val onMethodFilterSelected: () -> Unit,
    val onFeatureFilterSelected: () -> Unit,
    val onMappingPathFilterSelected: () -> Unit,
    val onMappingMethodFilterSelected: () -> Unit,
    val onChartDropdownOpening: () -> Unit
) {
    private lateinit var chartConfigComboBox: ComboBox<ChartConfig?>
    lateinit var methodFilterComboBox: ComboBox<String>
    lateinit var featureFilterComboBox: ComboBox<String>
    lateinit var mappingPathFilterComboBox: ComboBox<String>
    lateinit var mappingMethodFilterComboBox: ComboBox<String>
    private lateinit var openChartSettingsButton: JButton
    private lateinit var contextUpdateLockedButton: JToggleButton

    companion object {
        const val ALL_METHODS_OPTION = "All Methods"
        const val ALL_FEATURES_OPTION = "All Features"
        const val ALL_MAPPING_PATHS_OPTION = "All Paths"
        const val ALL_MAPPING_METHODS_OPTION = "All HTTP Methods"
        val LOCKED_ICON: Icon = IconLoader.getIcon("/icons/locked.svg", ChartControlsProvider::class.java)
        val UNLOCKED_ICON: Icon = IconLoader.getIcon("/icons/unlocked.svg", ChartControlsProvider::class.java)
    }

    fun createControlsPanel(): JPanel {
        chartConfigComboBox = ComboBox(chartConfigsModel).apply {
            renderer = object : SimpleListCellRenderer<ChartConfig?>() {
                override fun customize(
                    list: JList<out ChartConfig?>,
                    value: ChartConfig?,
                    index: Int,
                    selected: Boolean,
                    hasFocus: Boolean
                ) {
                    text = value?.name ?: if (itemCount == 0) "No Charts Configured" else "Select Chart"
                }
            }
            addItemListener { e ->
                if (e.stateChange == java.awt.event.ItemEvent.SELECTED) onChartConfigSelected()
            }
            addPopupMenuListener(object : PopupMenuListener {
                override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) = onChartDropdownOpening()
                override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {}
                override fun popupMenuCanceled(e: PopupMenuEvent?) {}
            })
        }

        methodFilterComboBox =
            createFilterComboBox(methodFilterModel, "Filter by method from current file", onMethodFilterSelected)
        featureFilterComboBox =
            createFilterComboBox(featureFilterModel, "Filter by feature from current file", onFeatureFilterSelected)
        mappingPathFilterComboBox = createFilterComboBox(
            mappingPathFilterModel,
            "Filter by mapping path from current context",
            onMappingPathFilterSelected
        )
        mappingMethodFilterComboBox = createFilterComboBox(
            mappingMethodFilterModel,
            "Filter by mapping HTTP method from current context",
            onMappingMethodFilterSelected
        )

        openChartSettingsButton = JButton(AllIcons.General.Settings).apply {
            toolTipText = "Open Chart Settings"
            addActionListener {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, ChartConfigurable::class.java)
            }
        }

        contextUpdateLockedButton = JToggleButton(UNLOCKED_ICON).apply {
            toolTipText = "Lock Context Update (Unlock to follow caret)"
            addActionListener {
                updateIsLockedButtonState(isSelected)
            }
        }

        return panel {
            row {
                label("Chart:").widthGroup("leftLabel")
                cell(chartConfigComboBox).resizableColumn().align(AlignX.FILL)
                cell(openChartSettingsButton)
                cell(contextUpdateLockedButton)
            }
            row {
                panel {
                    row {
                        label("Method FQN:").widthGroup("leftLabel")
                        cell(methodFilterComboBox).align(AlignX.FILL)
                    }
                    row {
                        label("Feature name:").widthGroup("leftLabel")
                        cell(featureFilterComboBox).align(AlignX.FILL)
                    }
                }.resizableColumn()
                panel {
                    row {
                        label("Mapping path:").widthGroup("rightLabel")
                        cell(mappingPathFilterComboBox).align(AlignX.FILL)
                    }
                    row {
                        label("HTTP method:").widthGroup("rightLabel")
                        cell(mappingMethodFilterComboBox).align(AlignX.FILL)
                    }
                }.resizableColumn()
            }
        }
    }

    fun getSelectedChartConfig(): ChartConfig? = chartConfigComboBox.selectedItem as? ChartConfig
    fun getSelectedMethodFilter(): String? = methodFilterComboBox.selectedItem as? String
    fun getSelectedFeatureFilter(): String? = featureFilterComboBox.selectedItem as? String
    fun getSelectedMappingPathFilter(): String? = mappingPathFilterComboBox.selectedItem as? String
    fun getSelectedMappingMethodFilter(): String? = mappingMethodFilterComboBox.selectedItem as? String

    private fun getFilteredItems(model: CollectionComboBoxModel<String>, exclude: String): List<String> =
        model.items.filter { it != exclude }

    fun getMethodsFqnsInFile() = getFilteredItems(methodFilterModel, ALL_METHODS_OPTION)
    fun getFeatureNamesInFile() = getFilteredItems(featureFilterModel, ALL_FEATURES_OPTION)
    fun getMappingPathsInFile() = getFilteredItems(mappingPathFilterModel, ALL_MAPPING_PATHS_OPTION)
    fun getMappingMethodsInFile() = getFilteredItems(mappingMethodFilterModel, ALL_MAPPING_METHODS_OPTION)

    fun isContextUpdateLocked(): Boolean = contextUpdateLockedButton.isSelected

    fun updateChartConfigComboBox(targetSelection: ChartConfig?, configs: List<ChartConfig>) {
        if (chartConfigsModel.items != configs) {
            chartConfigsModel.removeAll()
            chartConfigsModel.add(configs)
        }
        chartConfigComboBox.selectedItem =
            if (chartConfigsModel.items.contains(targetSelection)) targetSelection else chartConfigsModel.items.firstOrNull()
    }

    fun updateMethodFilterModel(targetSelection: String?, methodsInFile: List<String> = emptyList()) =
        updateFilterModel(methodFilterModel, methodFilterComboBox, targetSelection, methodsInFile, ALL_METHODS_OPTION)

    fun updateFeatureFilterModel(targetSelection: String?, featuresInFile: List<String> = emptyList()) =
        updateFilterModel(
            featureFilterModel,
            featureFilterComboBox,
            targetSelection,
            featuresInFile,
            ALL_FEATURES_OPTION
        )

    fun updateMappingPathFilterModel(targetSelection: String?, mappingPathsInFile: List<String> = emptyList()) =
        updateFilterModel(
            mappingPathFilterModel,
            mappingPathFilterComboBox,
            targetSelection,
            mappingPathsInFile,
            ALL_MAPPING_PATHS_OPTION
        )

    fun updateMappingMethodFilterModel(targetSelection: String?, mappingMethodsInFile: List<String> = emptyList()) =
        updateFilterModel(
            mappingMethodFilterModel,
            mappingMethodFilterComboBox,
            targetSelection,
            mappingMethodsInFile,
            ALL_MAPPING_METHODS_OPTION
        )

    fun updateIsLockedButtonState(isLocked: Boolean) {
        contextUpdateLockedButton.isSelected = isLocked
        contextUpdateLockedButton.icon = if (isLocked) LOCKED_ICON else UNLOCKED_ICON
        contextUpdateLockedButton.toolTipText =
            if (isLocked) "Context Update Locked (Click to Unlock)" else "Context Update Unlocked (Click to Lock)"
    }

    fun createStatusLabel(): JBLabel = JBLabel("Initializing...", JBLabel.CENTER)

    private fun <T> updateFilterModel(
        model: CollectionComboBoxModel<T>,
        comboBox: ComboBox<T>,
        targetSelection: T?,
        itemsInContext: List<T>,
        allOption: T
    ) {
        val distinctItems = (listOfNotNull(targetSelection) + itemsInContext).distinct()
        val withAllOption = listOf(allOption) + distinctItems.filterNot { it == allOption }
        if (model.items != withAllOption) {
            model.removeAll()
            model.add(withAllOption)
        }
        comboBox.selectedItem = targetSelection?.takeIf { model.items.contains(it) } ?: allOption
    }

    private fun <T> createFilterComboBox(
        model: CollectionComboBoxModel<T>,
        tooltip: String,
        onSelect: () -> Unit
    ): ComboBox<T> = ComboBox(model).apply {
        toolTipText = tooltip
        addItemListener { e -> if (e.stateChange == java.awt.event.ItemEvent.SELECTED) onSelect() }
    }
}
