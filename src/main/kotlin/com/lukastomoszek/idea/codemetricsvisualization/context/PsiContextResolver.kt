package com.lukastomoszek.idea.codemetricsvisualization.context

import com.intellij.psi.PsiElement
import com.lukastomoszek.idea.codemetricsvisualization.context.model.ContextInfo

object PsiContextResolver {

    suspend fun getContextInfoFromPsi(element: PsiElement): ContextInfo {
        val methodFqn = PsiUtils.getContainingMethodFqn(element)
        val featureName = FeatureExtractionUtil.getFeatureName(element)
        val (allMethodsInFile, allFeaturesInFile) = PsiFileUtils.getMethodsAndFeaturesInContainingFile(element)

        val (mappingPath, mappingMethod) = SpringMappingExtractionUtil.extractPathAndMethod(element)
        val (allMappingPathsInFile, allMappingMethodsInFile) = PsiFileUtils.extractMappingPathsAndMethods(element)

        return ContextInfo(
            methodFqn,
            featureName,
            allMethodsInFile,
            allFeaturesInFile,
            mappingPath,
            mappingMethod,
            allMappingPathsInFile,
            allMappingMethodsInFile
        )
    }
}
