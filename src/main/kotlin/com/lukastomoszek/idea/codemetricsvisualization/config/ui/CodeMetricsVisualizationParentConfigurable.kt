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

package com.lukastomoszek.idea.codemetricsvisualization.config.ui

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class CodeMetricsVisualizationParentConfigurable : Configurable {

    override fun getDisplayName(): String = "Code Metrics Visualization"

    override fun createComponent(): JComponent? = null

    override fun isModified(): Boolean = false

    override fun apply() {}
}
