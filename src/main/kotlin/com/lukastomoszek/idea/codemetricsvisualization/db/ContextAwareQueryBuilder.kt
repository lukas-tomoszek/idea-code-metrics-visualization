package com.lukastomoszek.idea.codemetricsvisualization.db

import com.lukastomoszek.idea.codemetricsvisualization.context.model.ContextInfo

object ContextAwareQueryBuilder {
    const val METHOD_FQN_PLACEHOLDER = "#method_fqn#"
    const val FEATURE_NAME_PLACEHOLDER = "#feature_name#"
    const val METHOD_FQNS_IN_FILE_PLACEHOLDER = "#method_fqns_in_file#"
    const val FEATURE_NAMES_IN_FILE_PLACEHOLDER = "#feature_names_in_file#"
    const val DEFAULT_PLACEHOLDER_VALUE = "%"

    private fun formatListToSqlArray(list: List<String>?): String {
        return if (list.isNullOrEmpty()) {
            "NULL"
        } else {
            list.joinToString { "'${it.replace("'", "''")}'" }
        }
    }

    fun buildQuery(
        sqlTemplate: String,
        contextInfo: ContextInfo,
        useDefaultsForUnresolved: Boolean = false
    ): Result<String> {
        var finalSql = sqlTemplate

        if (sqlTemplate.contains(METHOD_FQN_PLACEHOLDER)) {
            val value = when {
                contextInfo.methodFqn != null -> contextInfo.methodFqn
                useDefaultsForUnresolved -> DEFAULT_PLACEHOLDER_VALUE
                else -> return Result.failure(IllegalArgumentException("Missing required method FQN for SQL template"))
            }
            finalSql = finalSql.replace(METHOD_FQN_PLACEHOLDER, value)
        }

        if (sqlTemplate.contains(FEATURE_NAME_PLACEHOLDER)) {
            val value = when {
                contextInfo.featureName != null -> contextInfo.featureName
                useDefaultsForUnresolved -> DEFAULT_PLACEHOLDER_VALUE
                else -> return Result.failure(IllegalArgumentException("Missing required feature name for SQL template"))
            }
            finalSql = finalSql.replace(FEATURE_NAME_PLACEHOLDER, value)
        }

        if (sqlTemplate.contains(METHOD_FQNS_IN_FILE_PLACEHOLDER)) {
            finalSql =
                finalSql.replace(METHOD_FQNS_IN_FILE_PLACEHOLDER, formatListToSqlArray(contextInfo.allMethodsInFile))
        }

        if (sqlTemplate.contains(FEATURE_NAMES_IN_FILE_PLACEHOLDER)) {
            finalSql =
                finalSql.replace(FEATURE_NAMES_IN_FILE_PLACEHOLDER, formatListToSqlArray(contextInfo.allFeaturesInFile))
        }

        return Result.success(finalSql)
    }
}
