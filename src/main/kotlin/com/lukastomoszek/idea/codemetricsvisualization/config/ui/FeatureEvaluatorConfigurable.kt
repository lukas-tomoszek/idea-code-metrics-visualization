package com.lukastomoszek.idea.codemetricsvisualization.config.ui

import com.intellij.openapi.project.Project
import com.intellij.util.ui.ColumnInfo
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.FeatureEvaluatorSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.FeatureEvaluatorConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.FeatureParameterType
import com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog.AbstractDialog
import com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog.FeatureEvaluatorDialog

class FeatureEvaluatorConfigurable(project: Project) :
    AbstractListConfigurable<FeatureEvaluatorConfig>(
        project,
        "Feature Evaluators",
        "FeatureEvaluators",
        noItemsText = "No Feature Evaluators configured",
        commentText = "Define how feature evaluation methods look in your code to enable feature-specific queries in line markers."
    ) {

    private val nameColumn = object : ColumnInfo<FeatureEvaluatorConfig, String>("Name") {
        override fun valueOf(item: FeatureEvaluatorConfig): String = item.name
    }

    private val fqnColumn = object : ColumnInfo<FeatureEvaluatorConfig, String>("Method FQN") {
        override fun valueOf(item: FeatureEvaluatorConfig): String = item.evaluatorMethodFqn
    }

    private val paramIndexColumn = object : ColumnInfo<FeatureEvaluatorConfig, Int>("Param Index") {
        override fun valueOf(item: FeatureEvaluatorConfig): Int = item.featureParameterIndex
    }

    private val paramTypeColumn = object : ColumnInfo<FeatureEvaluatorConfig, FeatureParameterType>("Param Type") {
        override fun valueOf(item: FeatureEvaluatorConfig): FeatureParameterType = item.featureParameterType
    }

    override fun getColumnInfos(): Array<ColumnInfo<FeatureEvaluatorConfig, *>> =
        arrayOf(nameColumn, fqnColumn, paramIndexColumn, paramTypeColumn)

    override fun createNewItem(): FeatureEvaluatorConfig = FeatureEvaluatorConfig()

    override fun createEditDialog(item: FeatureEvaluatorConfig): AbstractDialog<FeatureEvaluatorConfig> {
        val otherNames = items.filterNot { it === item }.map { it.name }
        return FeatureEvaluatorDialog(project, item.copy(), otherNames)
    }

    override fun addItem() {
        val newItem = createNewItem()
        val currentNames = items.map { it.name }
        val dialog = FeatureEvaluatorDialog(project, newItem, currentNames)
        if (dialog.showAndGet()) {
            items.add(dialog.getUpdatedConfig())
            tableModel.fireTableDataChanged()
        }
    }

    override fun getItemsFromSettings(): List<FeatureEvaluatorConfig> =
        FeatureEvaluatorSettings.getInstance(project).state.featureEvaluators

    override fun saveItemsToSettings(items: List<FeatureEvaluatorConfig>) {
        FeatureEvaluatorSettings.getInstance(project).update(items)
    }

    override fun copyItem(item: FeatureEvaluatorConfig): FeatureEvaluatorConfig = item.copy()
}
