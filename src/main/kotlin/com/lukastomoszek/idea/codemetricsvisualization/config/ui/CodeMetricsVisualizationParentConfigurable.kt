package com.lukastomoszek.idea.codemetricsvisualization.config.ui

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class CodeMetricsVisualizationParentConfigurable : Configurable {

    override fun getDisplayName(): String = "Code Metrics Visualization"

    override fun createComponent(): JComponent? = null

    override fun isModified(): Boolean = false

    override fun apply() {}
}
