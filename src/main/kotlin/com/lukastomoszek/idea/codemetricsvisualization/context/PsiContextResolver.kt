package com.lukastomoszek.idea.codemetricsvisualization.context

import com.intellij.psi.PsiElement
import com.lukastomoszek.idea.codemetricsvisualization.context.model.ContextInfo

object PsiContextResolver {

    suspend fun getContextInfoFromPsi(element: PsiElement): ContextInfo {
        val project = element.project
        val methodFqn = PsiUtils.getContainingMethodFqn(element)
        val featureName = FeatureExtractionUtil.getFeatureName(element)
        val (allMethodsInFile, allFeaturesInFile) = EditorFileContextService.getInstance(project)
            .getMethodsAndFeaturesInContainingFile(element)
        return ContextInfo(methodFqn, featureName, allMethodsInFile, allFeaturesInFile)
    }
}
