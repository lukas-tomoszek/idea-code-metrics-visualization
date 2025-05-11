package com.lukastomoszek.idea.codemetricsvisualization.context

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.psi.*
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.FeatureEvaluatorSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.FeatureEvaluatorConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.FeatureParameterType

object FeatureExtractionUtil {

    fun getFeatureName(callExpression: PsiElement): String? {
        if (callExpression !is PsiMethodCallExpression) {
            thisLogger().trace("getFeatureName called with non-method-call element: ${callExpression.javaClass.simpleName}")
            return null
        }
        val project = callExpression.project
        val featureSettings = FeatureEvaluatorSettings.getInstance(project)

        val resolvedMethod = callExpression.resolveMethod() ?: return null.also {
            thisLogger().trace("Could not resolve method for call: ${callExpression.text}")
        }

        val calleeFqn = PsiUtils.getContainingMethodFqn(resolvedMethod)
        val matchingEvaluator = featureSettings.state.featureEvaluators.find { it.evaluatorMethodFqn == calleeFqn }

        return if (matchingEvaluator != null) {
            extractFeatureNameFromCall(callExpression, matchingEvaluator)
        } else {
            thisLogger().trace("No matching feature evaluator found for method FQN: $calleeFqn")
            null
        }
    }

    private fun extractFeatureNameFromCall(
        callExpression: PsiMethodCallExpression,
        config: FeatureEvaluatorConfig
    ): String? {
        val arguments = callExpression.argumentList.expressions
        if (config.featureParameterIndex < 0 || config.featureParameterIndex >= arguments.size) {
            thisLogger().warn("Feature parameter index ${config.featureParameterIndex} out of bounds for call ${callExpression.text} with ${arguments.size} arguments. Evaluator: ${config.name}")
            return null
        }
        val argument = arguments[config.featureParameterIndex]

        return try {
            when (config.featureParameterType) {
                FeatureParameterType.STRING -> {
                    (argument as? PsiLiteralExpression)?.value as? String
                }

                FeatureParameterType.ENUM_CONSTANT -> {
                    val reference = argument as? PsiReferenceExpression
                    val resolved = reference?.resolve()
                    (resolved as? PsiEnumConstant)?.name
                }
            }
        } catch (e: ClassCastException) {
            thisLogger().warn(
                "Argument type mismatch for feature extraction. Expected ${config.featureParameterType}, got ${argument.javaClass.simpleName} at index ${config.featureParameterIndex} in call ${callExpression.text}. Evaluator: ${config.name}",
                e
            )
            null
        } catch (e: Exception) {
            thisLogger().error(
                "Unexpected error extracting feature name from call ${callExpression.text} at index ${config.featureParameterIndex}. Evaluator: ${config.name}",
                e
            )
            null
        }
    }
}
