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
