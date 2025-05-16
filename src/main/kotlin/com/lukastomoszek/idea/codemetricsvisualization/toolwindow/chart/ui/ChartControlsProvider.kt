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
    val onChartConfigSelected: () -> Unit,
    val onMethodFilterSelected: () -> Unit,
    val onFeatureFilterSelected: () -> Unit,
    val onChartDropdownOpening: () -> Unit
) {
    private lateinit var chartConfigComboBox: ComboBox<ChartConfig?>
    lateinit var methodFilterComboBox: ComboBox<String>
    lateinit var featureFilterComboBox: ComboBox<String>
    private lateinit var openChartSettingsButton: JButton
    private lateinit var contextUpdateLockedButton: JToggleButton

    companion object {
        const val ALL_METHODS_OPTION = "All Methods"
        const val ALL_FEATURES_OPTION = "All Features"
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
                if (e.stateChange == java.awt.event.ItemEvent.SELECTED) {
                    onChartConfigSelected()
                }
            }
            addPopupMenuListener(object : PopupMenuListener {
                override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                    onChartDropdownOpening()
                }

                override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {}
                override fun popupMenuCanceled(e: PopupMenuEvent?) {}
            })
        }

        methodFilterComboBox = ComboBox(methodFilterModel).apply {
            toolTipText = "Filter by method from current file"
            addItemListener { e ->
                if (e.stateChange == java.awt.event.ItemEvent.SELECTED) {
                    onMethodFilterSelected()
                }
            }
        }

        featureFilterComboBox = ComboBox(featureFilterModel).apply {
            toolTipText = "Filter by feature from current file"
            addItemListener { e ->
                if (e.stateChange == java.awt.event.ItemEvent.SELECTED) {
                    onFeatureFilterSelected()
                }
            }
        }

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
            row("Chart:") {
                cell(chartConfigComboBox).resizableColumn().align(AlignX.FILL)
                cell(openChartSettingsButton)
                cell(contextUpdateLockedButton)
            }
            row("Method:") {
                cell(methodFilterComboBox).resizableColumn().align(AlignX.FILL)
            }
            row("Feature:") {
                cell(featureFilterComboBox).resizableColumn().align(AlignX.FILL)
            }
        }
    }

    fun getSelectedChartConfig(): ChartConfig? = chartConfigComboBox.selectedItem as? ChartConfig?
    fun getSelectedMethodFilter(): String? = methodFilterComboBox.selectedItem as? String
    fun getSelectedFeatureFilter(): String? = featureFilterComboBox.selectedItem as? String
    fun getMethodsFqnsInFile(): List<String> = methodFilterModel.items.filter { it != ALL_METHODS_OPTION }
    fun getFeatureNamesInFile(): List<String> = methodFilterModel.items.filter { it != ALL_FEATURES_OPTION }
    fun isContextUpdateLocked(): Boolean = contextUpdateLockedButton.isSelected

    fun updateChartConfigComboBox(targetSelection: ChartConfig?, configs: List<ChartConfig>) {
        if (chartConfigsModel.items != configs) {
            chartConfigsModel.removeAll()
            chartConfigsModel.add(configs)
        }

        chartConfigComboBox.selectedItem =
            if (chartConfigsModel.items.contains(targetSelection)) targetSelection else chartConfigsModel.items.firstOrNull()
    }

    fun updateMethodFilterModel(targetSelection: String?, methodsInFile: List<String> = emptyList()) {
        val withAllOption = methodsInFile
            .let { if (ALL_METHODS_OPTION in it) it else listOf(ALL_METHODS_OPTION) + it }

        if (methodFilterModel.items != withAllOption) {
            methodFilterModel.removeAll()
            methodFilterModel.add(withAllOption)
        }

        methodFilterComboBox.selectedItem =
            targetSelection?.takeIf { methodFilterModel.items.contains(it) } ?: ALL_METHODS_OPTION
    }

    fun updateFeatureFilterModel(targetSelection: String?, featuresInFile: List<String> = emptyList()) {
        val withAllOption = featuresInFile
            .let { if (ALL_FEATURES_OPTION in it) it else listOf(ALL_FEATURES_OPTION) + it }

        if (featureFilterModel.items != withAllOption) {
            featureFilterModel.removeAll()
            featureFilterModel.add(withAllOption)
        }

        featureFilterComboBox.selectedItem =
            targetSelection?.takeIf { featureFilterModel.items.contains(it) } ?: ALL_FEATURES_OPTION
    }

    fun updateIsLockedButtonState(isLocked: Boolean) {
        contextUpdateLockedButton.isSelected = isLocked
        contextUpdateLockedButton.icon = if (isLocked) LOCKED_ICON else UNLOCKED_ICON
        contextUpdateLockedButton.toolTipText =
            if (isLocked) "Context Update Locked (Click to Unlock)" else "Context Update Unlocked (Click to Lock)"
    }

    fun createStatusLabel(): JBLabel = JBLabel("Initializing...", JBLabel.CENTER)
}
