package com.lukastomoszek.idea.codemetricsvisualization.db

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import com.lukastomoszek.idea.codemetricsvisualization.util.FeatureExtractionUtil
import com.lukastomoszek.idea.codemetricsvisualization.util.PsiUtils

object ContextAwareQueryBuilder {
    const val METHOD_NAME_PLACEHOLDER = "#method_name#"
    const val FEATURE_NAME_PLACEHOLDER = "#feature_name#"

    fun buildQuery(
        sqlTemplate: String,
        anchorElement: PsiElement,
        useDefaultsForUnresolved: Boolean = false
    ): Result<String> {
        return ReadAction.compute<Result<String>, Throwable> {
            var finalSql = sqlTemplate

            if (sqlTemplate.contains(METHOD_NAME_PLACEHOLDER)) {
                val methodFqn = PsiUtils.getContainingMethodFqn(anchorElement)
                val value = when {
                    methodFqn != null -> methodFqn
                    useDefaultsForUnresolved -> "%"
                    else -> return@compute Result.failure(IllegalArgumentException("Missing required method FQN for SQL template, anchor: ${anchorElement.text}"))
                }
                finalSql = finalSql.replace(METHOD_NAME_PLACEHOLDER, value)
            }

            if (sqlTemplate.contains(FEATURE_NAME_PLACEHOLDER)) {
                val featureName = FeatureExtractionUtil.getFeatureName(anchorElement)
                val value = when {
                    featureName != null -> featureName
                    useDefaultsForUnresolved -> "%"
                    else -> return@compute Result.failure(IllegalArgumentException("Missing required feature name for SQL template, anchor: ${anchorElement.text}"))
                }
                finalSql = finalSql.replace(FEATURE_NAME_PLACEHOLDER, value)
            }
            Result.success(finalSql)
        }
    }
}
