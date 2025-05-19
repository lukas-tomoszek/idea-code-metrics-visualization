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
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

object PsiUtils {

    suspend fun getContainingMethodFqn(element: PsiElement): String? {
        return readAction {
            var currentElement: PsiElement? = element
            while (currentElement != null) {
                val method = PsiTreeUtil.getParentOfType(currentElement, PsiMethod::class.java, false)
                if (method == null) return@readAction null

                val containingClass = method.containingClass
                if (containingClass != null && containingClass !is PsiAnonymousClass) {
                    val methodName = method.name
                    val qualifiedClassName = containingClass.qualifiedName
                    return@readAction if (qualifiedClassName != null) "$qualifiedClassName.$methodName" else methodName
                }

                currentElement = method.parent
            }
            return@readAction null
        }
    }

    suspend fun getPsiFile(editor: Editor, project: Project): PsiFile? {
        return readAction {
            PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return@readAction null
        }
    }

    suspend fun findPsiElementAtOffset(psiFile: PsiFile, offset: Int): PsiElement? {
        return readAction {
            psiFile.findElementAt(offset)
        }
    }
}
