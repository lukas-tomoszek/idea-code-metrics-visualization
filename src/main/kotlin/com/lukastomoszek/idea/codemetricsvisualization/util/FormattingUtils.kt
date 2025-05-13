package com.lukastomoszek.idea.codemetricsvisualization.util

object FormattingUtils {
    fun formatNumber(number: Float): String =
        when {
            number % 1 == 0f -> String.format("%.0f", number)
            else -> String.format("%.2f", number)
        }
}
