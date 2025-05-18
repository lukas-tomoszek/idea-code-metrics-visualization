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
