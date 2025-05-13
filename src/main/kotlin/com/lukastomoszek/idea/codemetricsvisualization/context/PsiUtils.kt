package com.lukastomoszek.idea.codemetricsvisualization.context

import com.intellij.openapi.application.readAction
import com.intellij.psi.PsiElement
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
}
