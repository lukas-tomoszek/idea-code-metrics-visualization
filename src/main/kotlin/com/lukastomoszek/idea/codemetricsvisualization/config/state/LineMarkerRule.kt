package com.lukastomoszek.idea.codemetricsvisualization.config.state

data class LineMarkerRule(
    var operator: LineMarkerOperator = LineMarkerOperator.GREATER_THAN,
    var threshold: Float = 0.0f,
    var colorHex: String? = "#FF0000"
)

enum class LineMarkerOperator(val sign: String) {
    GREATER_THAN(">"),
    LESS_THAN("<"),
    EQUALS("=="),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN_OR_EQUAL("<="),
    NOT_EQUALS("!=");

    override fun toString(): String {
        return sign
    }
}
