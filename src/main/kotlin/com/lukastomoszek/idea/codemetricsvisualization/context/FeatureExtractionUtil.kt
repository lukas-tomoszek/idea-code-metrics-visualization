/*
 * Copyright (c) 2025 Lukáš Tomoszek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lukastomoszek.idea.codemetricsvisualization.context

import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.FeatureEvaluatorSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.FeatureEvaluatorConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.FeatureParameterType
import kotlinx.coroutines.CancellationException

object FeatureExtractionUtil {

    suspend fun getFeatureName(element: PsiElement): String? {
        val (callExpression, resolvedMethod) = readAction {
            val call = (element as? PsiMethodCallExpression)
                ?: PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression::class.java)
                ?: return@readAction null.also {
                    thisLogger().trace("Could not find PsiMethodCallExpression (self or parent) for element: ${element.text}")
                }

            val resolved = call.resolveMethod()
                ?: return@readAction null.also {
                    thisLogger().trace("Could not resolve method for call: ${call.text}")
                }

            call to resolved
        } ?: return null

        val project = callExpression.project
        val calleeFqn = PsiUtils.getContainingMethodFqn(resolvedMethod)
        val matchingEvaluator = FeatureEvaluatorSettings.getInstance(project).state.configs.find {
            it.evaluatorMethodFqn == calleeFqn
        }

        return if (matchingEvaluator != null) {
            extractFeatureNameFromCall(callExpression, matchingEvaluator)
        } else {
            thisLogger().trace("No matching feature evaluator found for method FQN: $calleeFqn")
            null
        }
    }

    private suspend fun extractFeatureNameFromCall(
        callExpression: PsiMethodCallExpression,
        config: FeatureEvaluatorConfig
    ): String? {
        return readAction {
            val arguments = callExpression.argumentList.expressions
            if (config.featureParameterIndex < 0 || config.featureParameterIndex >= arguments.size) {
                thisLogger().warn("Feature parameter index ${config.featureParameterIndex} out of bounds for call ${callExpression.text} with ${arguments.size} arguments. Evaluator: ${config.name}")
                return@readAction null
            }
            val argument = arguments[config.featureParameterIndex]

            return@readAction try {
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
                if (e is ControlFlowException || e is CancellationException) throw e
                thisLogger().error(
                    "Unexpected error extracting feature name from call ${callExpression.text} at index ${config.featureParameterIndex}. Evaluator: ${config.name}",
                    e
                )
                null
            }
        }
    }
}
