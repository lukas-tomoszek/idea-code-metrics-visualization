package com.lukastomoszek.idea.codemetricsvisualization.context

import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class EditorFileContextService(private val project: Project, private val cs: CoroutineScope) {

    suspend fun getMethodsAndFeaturesInContainingFile(psiElement: PsiElement): Pair<List<String>, List<String>> {
        val psiFile = readAction { psiElement.containingFile } ?: return Pair(emptyList(), emptyList())

        return withContext(cs.coroutineContext + Dispatchers.Default) {
            val methodsDeferred = async { collectMethods(psiFile) }
            val featuresDeferred = async { collectFeatures(psiFile) }

            val methods = methodsDeferred.await()
            val features = featuresDeferred.await()

            Pair(methods, features)
        }
    }

    private suspend fun collectMethods(psiFile: PsiFile): List<String> {
        val psiMethods = readAction {
            PsiTreeUtil.collectElementsOfType(psiFile, PsiMethod::class.java)
        }
        val methodFqns = psiMethods.mapNotNull { method ->
            PsiUtils.getContainingMethodFqn(method)
        }.distinct().sorted()
        return methodFqns
    }

    private suspend fun collectFeatures(psiFile: PsiFile): List<String> {
        val callExpressions = readAction {
            PsiTreeUtil.collectElementsOfType(psiFile, PsiMethodCallExpression::class.java)
        }
        val featureNames = callExpressions.mapNotNull { call ->
            FeatureExtractionUtil.getFeatureName(call)
        }.distinct().sorted()
        return featureNames
    }

    companion object {
        fun getInstance(project: Project): EditorFileContextService =
            project.getService(EditorFileContextService::class.java)
    }
}
