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
import com.intellij.ui.layout.ComponentPredicate
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
    val onChartConfigSelected: (ChartConfig?) -> Unit,
    val onMethodFilterSelected: (String?) -> Unit,
    val onFeatureFilterSelected: (String?) -> Unit,
    val onChartDropdownOpening: () -> Unit,
    val onMethodLockToggled: (Boolean) -> Unit,
    val onFeatureLockToggled: (Boolean) -> Unit,
    val isMethodFilterEnabled: () -> Boolean,
    val isFeatureFilterEnabled: () -> Boolean
) {
    lateinit var chartConfigComboBox: ComboBox<ChartConfig?>
    lateinit var methodFilterComboBox: ComboBox<String>
    lateinit var featureFilterComboBox: ComboBox<String>
    lateinit var openChartSettingsButton: JButton
    lateinit var lockMethodButton: JToggleButton
    lateinit var lockFeatureButton: JToggleButton

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
                    onChartConfigSelected(selectedItem as? ChartConfig)
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
                    onMethodFilterSelected(selectedItem as? String)
                }
            }
        }

        featureFilterComboBox = ComboBox(featureFilterModel).apply {
            toolTipText = "Filter by feature from current file"
            addItemListener { e ->
                if (e.stateChange == java.awt.event.ItemEvent.SELECTED) {
                    onFeatureFilterSelected(selectedItem as? String)
                }
            }
        }

        openChartSettingsButton = JButton(AllIcons.General.Settings).apply {
            toolTipText = "Open Chart Settings"
            addActionListener {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, ChartConfigurable::class.java)
            }
        }

        lockMethodButton = JToggleButton(UNLOCKED_ICON).apply {
            toolTipText = "Lock Method Filter (Unlock to follow caret)"
            addActionListener {
                onMethodLockToggled(isSelected)
                updateLockButton(this, isSelected)
            }
        }

        lockFeatureButton = JToggleButton(UNLOCKED_ICON).apply {
            toolTipText = "Lock Feature Filter (Unlock to follow caret)"
            addActionListener {
                onFeatureLockToggled(isSelected)
                updateLockButton(this, isSelected)
            }
        }

        return panel {
            row("Chart:") {
                cell(chartConfigComboBox).resizableColumn().align(AlignX.FILL)
                cell(openChartSettingsButton)
            }
            row("Method:") {
                cell(methodFilterComboBox).resizableColumn().align(AlignX.FILL)
                    .enabledIf(ComponentPredicate.fromValue(isMethodFilterEnabled()))
                cell(lockMethodButton)
                    .enabledIf(ComponentPredicate.fromValue(isMethodFilterEnabled()))
            }
            row("Feature:") {
                cell(featureFilterComboBox).resizableColumn().align(AlignX.FILL)
                    .enabledIf(ComponentPredicate.fromValue(isFeatureFilterEnabled()))
                cell(lockFeatureButton)
                    .enabledIf(ComponentPredicate.fromValue(isFeatureFilterEnabled()))
            }
        }
    }

    fun updateLockButton(button: JToggleButton, isLocked: Boolean) {
        button.icon = if (isLocked) LOCKED_ICON else UNLOCKED_ICON
        button.toolTipText = if (isLocked) "Filter Locked (Click to Unlock)" else "Filter Unlocked (Click to Lock)"
        button.isSelected = isLocked
    }

    fun createStatusLabel(): JBLabel = JBLabel("Initializing...", JBLabel.CENTER)
}
