package com.lukastomoszek.idea.codemetricsvisualization.linemarker.rule

import com.intellij.openapi.diagnostic.thisLogger
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerRule
import java.awt.Color

object RuleEvaluator {

    fun evaluate(value: Float?, rules: List<LineMarkerRule>): Color? {
        if (value == null) {
            return null
        }

        for (rule in rules) {
            try {
                val threshold = rule.threshold
                val match = when (rule.operator) {
                    ">" -> value > threshold
                    "<" -> value < threshold
                    "==" -> value == threshold
                    ">=" -> value >= threshold
                    "<=" -> value <= threshold
                    "!=" -> value != threshold
                    else -> {
                        thisLogger().warn("Unsupported operator in rule: ${rule.operator}")
                        false
                    }
                }

                if (match) {
                    val hexColor = rule.colorHex
                    if (hexColor.isNullOrBlank()) {
                        return null
                    }

                    val color = parseColor(hexColor)
                    if (color != null) {
                        return color
                    } else {
                        thisLogger().warn("Failed to parse color '$hexColor' in rule: $rule. Skipping rule.")
                    }
                }
            } catch (e: Exception) {
                thisLogger().error("Error evaluating rule: $rule for value: $value", e)
            }
        }
        return null
    }

    private fun parseColor(hexColor: String): Color? {
        if (hexColor.isBlank()) return null
        val hex = hexColor.trim()
        return try {
            Color.decode(if (hex.startsWith("#")) hex else "#$hex")
        } catch (e: NumberFormatException) {
            thisLogger().warn("Invalid color format: '$hexColor'", e)
            null
        }
    }
}
