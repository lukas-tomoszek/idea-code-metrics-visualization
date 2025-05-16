package com.lukastomoszek.idea.codemetricsvisualization.linemarker.provider

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig

class MethodDefinitionLineMarkerProvider : AbstractMetricLineMarkerProvider<PsiIdentifier>(PsiIdentifier::class.java) {

    override suspend fun preFilterElement(element: PsiIdentifier, project: Project): Boolean {
        return readAction {
            element.parent is PsiMethod && (element.parent as PsiMethod).nameIdentifier == element
        }
    }

    override fun filterEnabledConfigs(allEnabledConfigs: List<LineMarkerConfig>): List<LineMarkerConfig> {
        return allEnabledConfigs.filter { it.hasMethodFqnPlaceholder() && !it.hasFeatureNamePlaceholder() }
    }

    override suspend fun getAnchorElement(element: PsiIdentifier): PsiElement? {
        return readAction {
            (element.parent as? PsiMethod)?.nameIdentifier
        }
    }
}
