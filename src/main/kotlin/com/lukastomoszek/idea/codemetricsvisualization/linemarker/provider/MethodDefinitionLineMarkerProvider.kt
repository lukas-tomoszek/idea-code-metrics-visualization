package com.lukastomoszek.idea.codemetricsvisualization.linemarker.provider

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder

class MethodDefinitionLineMarkerProvider : AbstractMetricLineMarkerProvider<PsiIdentifier>(PsiIdentifier::class.java) {

    override fun preFilterElement(element: PsiIdentifier, project: Project): Boolean {
        return element.parent is PsiMethod && (element.parent as PsiMethod).nameIdentifier == element
    }

    override fun filterEnabledConfigs(allEnabledConfigs: List<LineMarkerConfig>): List<LineMarkerConfig> {
        return allEnabledConfigs.filter {
            it.sqlTemplate.contains(ContextAwareQueryBuilder.METHOD_NAME_PLACEHOLDER) &&
                    !it.sqlTemplate.contains(ContextAwareQueryBuilder.FEATURE_NAME_PLACEHOLDER)
        }
    }

    override fun getAnchorElement(element: PsiIdentifier): PsiElement? {
        return (element.parent as? PsiMethod)?.nameIdentifier
    }

    override fun getLineMarkerGroupName(config: LineMarkerConfig): String {
        return "Code Metric Visualization (${config.name})"
    }
}
