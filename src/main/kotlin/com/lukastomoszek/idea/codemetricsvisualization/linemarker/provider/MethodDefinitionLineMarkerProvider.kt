package com.lukastomoszek.idea.codemetricsvisualization.linemarker.provider

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder

class MethodDefinitionLineMarkerProvider : AbstractMetricLineMarkerProvider<PsiIdentifier>(PsiIdentifier::class.java) {

    override fun filterEnabledConfigs(configs: List<LineMarkerConfig>): List<LineMarkerConfig> {
        return configs.filter {
            it.sqlTemplate.contains(ContextAwareQueryBuilder.METHOD_FQN_PLACEHOLDER)
            && !it.sqlTemplate.contains(ContextAwareQueryBuilder.FEATURE_NAME_PLACEHOLDER)
            && !it.sqlTemplate.contains(ContextAwareQueryBuilder.MAPPING_PATH_PLACEHOLDER)
            && !it.sqlTemplate.contains(ContextAwareQueryBuilder.MAPPING_METHOD_PLACEHOLDER)
        }
    }

    override suspend fun preFilterElement(element: PsiIdentifier, project: Project): Boolean {
        return readAction {
            element.parent is PsiMethod && (element.parent as PsiMethod).nameIdentifier == element
        }
    }

    override suspend fun getAnchorElement(element: PsiIdentifier): PsiElement? {
        return readAction {
            (element.parent as? PsiMethod)?.nameIdentifier
        }
    }
}
