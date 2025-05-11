package com.lukastomoszek.idea.codemetricsvisualization.config.state

data class LineMarkerRule(
    var operator: String = ">",
    var threshold: Float = 0.0f,
    var colorHex: String? = "#FF0000"
)
