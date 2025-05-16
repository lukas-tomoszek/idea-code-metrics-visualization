package com.lukastomoszek.idea.codemetricsvisualization.context.model

data class ContextInfo(
    val methodFqn: String?,
    val featureName: String?,
    val allMethodsInFile: List<String>,
    val allFeaturesInFile: List<String>
)
