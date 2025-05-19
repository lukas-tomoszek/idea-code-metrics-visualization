/*
 * Copyright (c) 2025 Lukáš Tomoszek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lukastomoszek.idea.codemetricsvisualization.context

import com.intellij.openapi.application.readAction
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

object PsiFileUtils {

    suspend fun getMethodsAndFeaturesInContainingFile(psiElement: PsiElement): Pair<List<String>, List<String>> {
        val psiFile = readAction { psiElement.containingFile } ?: return Pair(emptyList(), emptyList())

        return withContext(Dispatchers.Default) {
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

    suspend fun extractMappingPathsAndMethods(psiElement: PsiElement): Pair<List<String>, List<String>> {
        val psiFile = readAction { psiElement.containingFile } ?: return emptyList<String>() to emptyList()

        val annotations = readAction {
            PsiTreeUtil.collectElementsOfType(psiFile, PsiAnnotation::class.java)
        }

        val results = annotations.mapNotNull { annotation ->
            if (SpringMappingExtractionUtil.isSpringMappingAnnotation(readAction { annotation.qualifiedName })) {
                SpringMappingExtractionUtil.extractPathAndMethod(annotation)
            } else null
        }

        val paths = results.mapNotNull { it.first }.distinct().sorted()
        val methods = results.mapNotNull { it.second }.distinct().sorted()
        return paths to methods
    }
}
