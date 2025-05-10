package com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper

abstract class AbstractDialog<ConfigType : Any>(project: Project) : DialogWrapper(project) {

    abstract fun getUpdatedConfig(): ConfigType
}
