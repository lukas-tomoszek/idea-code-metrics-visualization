package com.lukastomoszek.idea.codemetricsvisualization.db

import com.lukastomoszek.idea.codemetricsvisualization.context.model.ContextInfo

object ContextAwareQueryBuilder {
    const val METHOD_NAME_PLACEHOLDER = "#method_name#"
    const val FEATURE_NAME_PLACEHOLDER = "#feature_name#"

    fun buildQuery(
        sqlTemplate: String,
        contextInfo: ContextInfo
    ): Result<String> {
        var finalSql = sqlTemplate

        if (sqlTemplate.contains(METHOD_NAME_PLACEHOLDER)) {
            val value = when {
                contextInfo.methodFqn != null -> contextInfo.methodFqn
                else -> return Result.failure(IllegalArgumentException("Missing required method FQN for SQL template"))
            }
            finalSql = finalSql.replace(METHOD_NAME_PLACEHOLDER, value)
        }

        if (sqlTemplate.contains(FEATURE_NAME_PLACEHOLDER)) {
            val value = when {
                contextInfo.featureName != null -> contextInfo.featureName
                else -> return Result.failure(IllegalArgumentException("Missing required feature name for SQL template"))
            }
            finalSql = finalSql.replace(FEATURE_NAME_PLACEHOLDER, value)
        }
        return Result.success(finalSql)
    }
}
