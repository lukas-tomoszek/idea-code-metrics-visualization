package com.lukastomoszek.idea.codemetricsvisualization.linemarker.provider

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethodCallExpression
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.FeatureEvaluatorSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder
import com.lukastomoszek.idea.codemetricsvisualization.util.FeatureExtractionUtil
import com.lukastomoszek.idea.codemetricsvisualization.util.PsiUtils

class FeatureEvaluationLineMarkerProvider :
    AbstractMetricLineMarkerProvider<PsiMethodCallExpression>(PsiMethodCallExpression::class.java) {

    override fun filterEnabledConfigs(allEnabledConfigs: List<LineMarkerConfig>): List<LineMarkerConfig> {
        return allEnabledConfigs.filter {
            it.sqlTemplate.contains(ContextAwareQueryBuilder.FEATURE_NAME_PLACEHOLDER)
        }
    }

    override fun preFilterElement(element: PsiMethodCallExpression, project: Project): Boolean {
        return ReadAction.compute<Boolean, Throwable> {
            val featureEvaluatorSettings = FeatureEvaluatorSettings.getInstance(project)
            val configuredEvaluatorFqns =
                featureEvaluatorSettings.state.featureEvaluators.map { it.evaluatorMethodFqn }.toSet()

            if (configuredEvaluatorFqns.isEmpty()) return@compute false

            val resolvedMethod = element.resolveMethod()
            if (resolvedMethod != null) {
                val methodFqn = PsiUtils.getContainingMethodFqn(resolvedMethod)
                methodFqn != null &&
                        configuredEvaluatorFqns.contains(methodFqn) &&
                        FeatureExtractionUtil.getFeatureName(element) != null
            } else {
                false
            }
        }
    }

    override fun getAnchorElement(element: PsiMethodCallExpression): PsiElement? {
        return element.methodExpression.referenceNameElement ?: element.methodExpression
    }

    override fun getLineMarkerGroupName(config: LineMarkerConfig): String {
        return "Metric Visualization (${config.name})"
    }
}
