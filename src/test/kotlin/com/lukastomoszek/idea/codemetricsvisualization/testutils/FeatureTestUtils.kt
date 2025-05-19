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

package com.lukastomoszek.idea.codemetricsvisualization.testutils

import com.intellij.openapi.project.Project
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.FeatureEvaluatorSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.FeatureEvaluatorConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.FeatureParameterType

fun setupFeatureEvaluatorSettings(project: Project, configs: List<FeatureEvaluatorConfig>) {
    FeatureEvaluatorSettings.getInstance(project).update(configs)
}

fun clearFeatureEvaluatorSettings(project: Project) {
    FeatureEvaluatorSettings.getInstance(project).update(emptyList())
}

object FeatureEvaluatorTestConfigs {
    val stringEvaluator = FeatureEvaluatorConfig(
        name = "String Evaluator",
        evaluatorMethodFqn = "com.example.features.FeatureClient.getBooleanValue",
        featureParameterIndex = 0,
        featureParameterType = FeatureParameterType.STRING
    )

    val enumEvaluator = FeatureEvaluatorConfig(
        name = "Enum Evaluator",
        evaluatorMethodFqn = "com.example.features.FeatureClient.isEnabled",
        featureParameterIndex = 0,
        featureParameterType = FeatureParameterType.ENUM_CONSTANT
    )

    val secondParamStringEvaluator = FeatureEvaluatorConfig(
        name = "String Evaluator Second Param",
        evaluatorMethodFqn = "com.example.features.FeatureClient.getStringValue",
        featureParameterIndex = 1,
        featureParameterType = FeatureParameterType.STRING
    )

    val secondParamEnumEvaluator = FeatureEvaluatorConfig(
        name = "Enum Evaluator Second Param",
        evaluatorMethodFqn = "com.example.features.FeatureClient.getIntValue",
        featureParameterIndex = 1,
        featureParameterType = FeatureParameterType.ENUM_CONSTANT
    )
}
