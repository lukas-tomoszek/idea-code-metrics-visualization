package com.lukastomoszek.idea.codemetricsvisualization.linemarker.provider

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethodCallExpression
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.FeatureEvaluatorSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.context.FeatureExtractionUtil
import com.lukastomoszek.idea.codemetricsvisualization.context.PsiUtils
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder

class FeatureEvaluationLineMarkerProvider :
    AbstractMetricLineMarkerProvider<PsiMethodCallExpression>(PsiMethodCallExpression::class.java) {

    override fun filterEnabledConfigs(allEnabledConfigs: List<LineMarkerConfig>): List<LineMarkerConfig> {
        return allEnabledConfigs.filter {
            it.sqlTemplate.contains(ContextAwareQueryBuilder.FEATURE_NAME_PLACEHOLDER)
        }
    }

    override suspend fun preFilterElement(element: PsiMethodCallExpression, project: Project): Boolean {
        val featureEvaluatorSettings = FeatureEvaluatorSettings.getInstance(project)
        val configuredEvaluatorFqns =
            featureEvaluatorSettings.state.featureEvaluators.map { it.evaluatorMethodFqn }.toSet()

        if (configuredEvaluatorFqns.isEmpty()) false
        val resolvedMethod = readAction {
            element.resolveMethod()
        }
        if (resolvedMethod != null) {
            val methodFqn = PsiUtils.getContainingMethodFqn(resolvedMethod)
            return methodFqn != null &&
                    configuredEvaluatorFqns.contains(methodFqn) &&
                    FeatureExtractionUtil.getFeatureName(element) != null
        } else {
            return false
        }
    }

    override suspend fun getAnchorElement(element: PsiMethodCallExpression): PsiElement? {
        return readAction {
            element.methodExpression.referenceNameElement ?: element.methodExpression
        }
    }

    override fun getLineMarkerGroupName(config: LineMarkerConfig): String {
        return "Metric Visualization (${config.name})"
    }
}
