package com.lukastomoszek.idea.codemetricsvisualization.linemarker.rule

import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.CancellationException
import java.awt.Color

object RuleEvaluator {

    fun parseBoundaryString(boundaryStringInput: String, isFromBoundary: Boolean): Float {
        val boundaryString = boundaryStringInput.trim()
        return when {
            boundaryString.isEmpty() -> {
                if (isFromBoundary) Float.NEGATIVE_INFINITY else Float.POSITIVE_INFINITY
            }

            else -> try {
                boundaryString.toFloat()
            } catch (e: NumberFormatException) {
                thisLogger().warn("Cannot parse rule boundary value '$boundaryStringInput'", e)
                throw e
            }
        }
    }

    fun evaluate(value: Float?, rules: List<RangeRule>): Color? {
        if (value == null) {
            return null
        }

        for (rule in rules) {
            try {
                val fromVal = parseBoundaryString(rule.fromString, true)
                val toVal = parseBoundaryString(rule.toString, false)

                if (value > fromVal && value <= toVal) {
                    val hexColor = rule.colorHex
                    if (hexColor.isBlank()) {
                        return null
                    }
                    return parseColor(hexColor) ?: continue
                }
            } catch (e: Exception) {
                if (e is ControlFlowException || e is CancellationException) throw e
                thisLogger().error("Error evaluating rule: $rule for value: $value", e)
            }
        }
        return null
    }

    private fun parseColor(hexColor: String): Color? {
        if (hexColor.isBlank()) return null
        val hex = hexColor.trim()
        return try {
            Color.decode(hex)
        } catch (e: NumberFormatException) {
            thisLogger().warn("Invalid color format: '$hexColor'", e)
            null
        }
    }
}
