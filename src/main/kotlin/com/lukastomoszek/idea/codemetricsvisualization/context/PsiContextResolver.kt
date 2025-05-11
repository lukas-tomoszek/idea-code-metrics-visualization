package com.lukastomoszek.idea.codemetricsvisualization.context

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import com.lukastomoszek.idea.codemetricsvisualization.context.model.ContextInfo

object PsiContextResolver {

    fun getContextInfoFromPsi(element: PsiElement): ContextInfo {
        return ReadAction.compute<ContextInfo, Throwable> {
            val methodFqn = PsiUtils.getContainingMethodFqn(element)
            val featureName = FeatureExtractionUtil.getFeatureName(element)
            ContextInfo(methodFqn, featureName)
        }
    }
}
