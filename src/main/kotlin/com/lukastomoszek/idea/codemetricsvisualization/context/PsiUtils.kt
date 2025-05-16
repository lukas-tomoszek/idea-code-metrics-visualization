package com.lukastomoszek.idea.codemetricsvisualization.context

import com.intellij.openapi.application.readAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil

object PsiUtils {

    suspend fun getContainingMethodFqn(element: PsiElement): String? {
        return readAction {
            val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java, false) ?: return@readAction null
            val containingClass = method.containingClass ?: return@readAction null

            val methodName = method.name
            val qualifiedClassName = containingClass.qualifiedName

            if (qualifiedClassName != null) {
                "$qualifiedClassName.$methodName"
            } else {
                methodName
            }
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
