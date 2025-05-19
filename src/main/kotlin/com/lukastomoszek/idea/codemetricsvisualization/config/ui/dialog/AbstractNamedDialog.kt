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
