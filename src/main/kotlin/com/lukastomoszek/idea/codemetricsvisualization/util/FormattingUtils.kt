package com.lukastomoszek.idea.codemetricsvisualization.util

object FormattingUtils {
    fun formatNumber(number: Float?): String {
        return number?.let { if (it % 1 == 0.0F) String.format("%.0f", it) else String.format("%.2f", it) } ?: "N/A"
    }
}
