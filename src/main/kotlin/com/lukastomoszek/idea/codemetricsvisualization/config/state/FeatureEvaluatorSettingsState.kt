package com.lukastomoszek.idea.codemetricsvisualization.config.state

import com.intellij.util.xmlb.annotations.XCollection

internal object DefaultFeatureEvaluatorConfig {
    const val NAME = "New Feature Evaluator"
    const val EVALUATOR_METHOD_FQN = "dev.openfeature.sdk.Client.getBooleanValue"
    const val FEATURE_PARAMETER_INDEX = 0
    val FEATURE_PARAMETER_TYPE = FeatureParameterType.STRING
}

data class FeatureEvaluatorSettingsState(
    @XCollection
    val featureEvaluators: List<FeatureEvaluatorConfig> = listOf()
)

enum class FeatureParameterType {
    STRING, ENUM_CONSTANT
}

data class FeatureEvaluatorConfig(
    var name: String = DefaultFeatureEvaluatorConfig.NAME,
    var evaluatorMethodFqn: String = DefaultFeatureEvaluatorConfig.EVALUATOR_METHOD_FQN,
    var featureParameterIndex: Int = DefaultFeatureEvaluatorConfig.FEATURE_PARAMETER_INDEX,
    var featureParameterType: FeatureParameterType = DefaultFeatureEvaluatorConfig.FEATURE_PARAMETER_TYPE
)
