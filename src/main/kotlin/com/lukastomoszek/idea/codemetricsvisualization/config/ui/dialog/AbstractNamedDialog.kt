package com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.lukastomoszek.idea.codemetricsvisualization.config.state.NamedConfig

abstract class AbstractNamedDialog<ConfigType : NamedConfig>(
    project: Project,
    protected val config: ConfigType,
    private val existingConfigNames: List<String>
) :
    DialogWrapper(project) {

    private val originalName = config.name
    protected lateinit var nameField: JBTextField

    protected fun validateName(name: String): ValidationInfo? {
        if (name.isBlank()) {
            return ValidationInfo("Name cannot be empty", nameField)
        }
        if (name != originalName && existingConfigNames.any { it.equals(name, ignoreCase = true) }) {
            return ValidationInfo("A config with this name already exists.", nameField)
        }
        return null
    }

    fun getUpdatedConfig(): ConfigType = config
}
