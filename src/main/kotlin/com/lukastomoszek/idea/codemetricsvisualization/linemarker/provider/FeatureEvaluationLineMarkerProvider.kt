package com.lukastomoszek.idea.codemetricsvisualization.linemarker.provider

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethodCallExpression
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.FeatureEvaluatorSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.context.PsiUtils
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder

class FeatureEvaluationLineMarkerProvider :
    AbstractMetricLineMarkerProvider<PsiMethodCallExpression>(PsiMethodCallExpression::class.java) {

    override fun filterEnabledConfigs(allEnabledConfigs: List<LineMarkerConfig>): List<LineMarkerConfig> {
        return allEnabledConfigs.filter { it.sqlTemplate.contains(ContextAwareQueryBuilder.FEATURE_NAME_PLACEHOLDER) }
    }

    override suspend fun preFilterElement(element: PsiMethodCallExpression, project: Project): Boolean {
        val configuredFqns = FeatureEvaluatorSettings.getInstance(project).state.configs
            .map { it.evaluatorMethodFqn }
            .toSet()

        if (configuredFqns.isEmpty()) return false

        val method = readAction { element.resolveMethod() } ?: return false
        val methodFqn = PsiUtils.getContainingMethodFqn(method)

        return methodFqn != null && methodFqn in configuredFqns
    }

    override suspend fun getAnchorElement(element: PsiMethodCallExpression): PsiElement? {
        return readAction {
            element.methodExpression.referenceNameElement ?: element.methodExpression
        }
    }
}
