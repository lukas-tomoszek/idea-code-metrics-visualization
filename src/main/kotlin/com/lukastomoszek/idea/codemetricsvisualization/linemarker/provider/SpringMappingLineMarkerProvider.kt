package com.lukastomoszek.idea.codemetricsvisualization.linemarker.provider

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.context.SpringMappingExtractionUtil
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder

class SpringMappingLineMarkerProvider : AbstractMetricLineMarkerProvider<PsiAnnotation>(PsiAnnotation::class.java) {

    override fun filterEnabledConfigs(configs: List<LineMarkerConfig>): List<LineMarkerConfig> {
        return configs.filter {
            (it.sqlTemplate.contains(ContextAwareQueryBuilder.MAPPING_PATH_PLACEHOLDER)
             || it.sqlTemplate.contains(ContextAwareQueryBuilder.MAPPING_METHOD_PLACEHOLDER))
            && !it.sqlTemplate.contains(ContextAwareQueryBuilder.FEATURE_NAME_PLACEHOLDER)
        }
    }

    override suspend fun preFilterElement(element: PsiAnnotation, project: Project): Boolean {
        return readAction { SpringMappingExtractionUtil.isSpringMappingAnnotation(element.qualifiedName) }
    }

    override suspend fun getAnchorElement(element: PsiAnnotation): PsiElement? = readAction {
        element.nameReferenceElement?.referenceNameElement ?: element.nameReferenceElement ?: element
    }
}
