package com.lukastomoszek.idea.codemetricsvisualization.context

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil

object PsiUtils {

    fun getContainingMethodFqn(element: PsiElement): String? {
        val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java, false) ?: return null
        val containingClass = method.containingClass ?: return null

        val methodName = method.name
        val qualifiedClassName = containingClass.qualifiedName

        return if (qualifiedClassName != null) {
            "$qualifiedClassName.$methodName"
        } else {
            methodName
        }
    }
}
