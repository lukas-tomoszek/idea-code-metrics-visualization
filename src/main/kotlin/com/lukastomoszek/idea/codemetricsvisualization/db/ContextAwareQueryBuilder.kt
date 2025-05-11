package com.lukastomoszek.idea.codemetricsvisualization.db

import com.intellij.psi.PsiElement
import com.lukastomoszek.idea.codemetricsvisualization.util.PsiUtils

object ContextAwareQueryBuilder {
    const val METHOD_NAME_PLACEHOLDER = "#method_name#"

    fun buildQuery(
        sqlTemplate: String,
        anchorElement: PsiElement,
        useDefaults: Boolean = false
    ): Result<String> {
        var finalSql = sqlTemplate
        val requiresMethod = sqlTemplate.contains(METHOD_NAME_PLACEHOLDER)

        if (requiresMethod) {
            val methodFqn = PsiUtils.getContainingMethodFqn(anchorElement)
            val value = when {
                methodFqn != null -> methodFqn
                useDefaults -> "%"
                else -> return Result.failure(IllegalArgumentException("Missing required method FQN for SQL template, anchor: ${anchorElement.text}"))
            }
            finalSql = finalSql.replace(METHOD_NAME_PLACEHOLDER, value)
        }
        return Result.success(finalSql)
    }
}
