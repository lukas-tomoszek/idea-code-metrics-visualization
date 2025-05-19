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

package com.lukastomoszek.idea.codemetricsvisualization.config.state

import com.intellij.util.xmlb.annotations.XCollection

enum class FeatureParameterType {
    STRING, ENUM_CONSTANT
}

internal object DefaultFeatureEvaluatorConfig {
    const val NAME = "New Feature Evaluator"
    const val EVALUATOR_METHOD_FQN = "dev.openfeature.sdk.Client.getBooleanValue"
    const val FEATURE_PARAMETER_INDEX = 0
    val FEATURE_PARAMETER_TYPE = FeatureParameterType.STRING
}

data class FeatureEvaluatorConfig(
    override var name: String = DefaultFeatureEvaluatorConfig.NAME,
    var evaluatorMethodFqn: String = DefaultFeatureEvaluatorConfig.EVALUATOR_METHOD_FQN,
    var featureParameterIndex: Int = DefaultFeatureEvaluatorConfig.FEATURE_PARAMETER_INDEX,
    var featureParameterType: FeatureParameterType = DefaultFeatureEvaluatorConfig.FEATURE_PARAMETER_TYPE
) : NamedConfig

data class FeatureEvaluatorSettingsState(
    @XCollection
    val configs: List<FeatureEvaluatorConfig> = listOf()
)
