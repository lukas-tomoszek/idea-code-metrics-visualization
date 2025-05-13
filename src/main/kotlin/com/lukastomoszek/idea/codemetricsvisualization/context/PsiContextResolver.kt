package com.lukastomoszek.idea.codemetricsvisualization.context

import com.intellij.psi.PsiElement
import com.lukastomoszek.idea.codemetricsvisualization.context.model.ContextInfo

object PsiContextResolver {

    suspend fun getContextInfoFromPsi(element: PsiElement): ContextInfo {
        val methodFqn = PsiUtils.getContainingMethodFqn(element)
        val featureName = FeatureExtractionUtil.getFeatureName(element)
        return ContextInfo(methodFqn, featureName)
    }
}
