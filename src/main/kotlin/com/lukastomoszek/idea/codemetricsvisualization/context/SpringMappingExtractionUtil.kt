package com.lukastomoszek.idea.codemetricsvisualization.context

import com.intellij.openapi.application.readAction
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

object SpringMappingExtractionUtil {

    private val SPRING_MAPPINGS = mapOf(
        "org.springframework.web.bind.annotation.GetMapping" to "GET",
        "org.springframework.web.bind.annotation.PostMapping" to "POST",
        "org.springframework.web.bind.annotation.PutMapping" to "PUT",
        "org.springframework.web.bind.annotation.DeleteMapping" to "DELETE",
        "org.springframework.web.bind.annotation.PatchMapping" to "PATCH",
        "org.springframework.web.bind.annotation.RequestMapping" to null
    )
    private const val DEFAULT_METHOD = "GET"

    private const val VALUE = "value"
    private const val PATH = "path"
    private const val METHOD = "method"

    suspend fun extractPathAndMethod(element: PsiElement): Pair<String?, String?> {
        val method = readAction { PsiTreeUtil.getParentOfType(element, PsiMethod::class.java) } ?: return null to null
        val annotation = element as? PsiAnnotation ?: getMethodAnnotation(method) ?: return null to null

        val classAnnotation = getContainingClassAnnotation(method)
        val classPath = getPath(classAnnotation).orEmpty()
        val methodPath = getPath(annotation).orEmpty()
        val combined = combinePaths(classPath, methodPath)

        val methodType = getHttpMethod(annotation)
        return convertSpringPlaceholdersToRegex(combined) to methodType
    }

    private suspend fun getMethodAnnotation(method: PsiMethod): PsiAnnotation? = readAction {
        method.modifierList.annotations.firstOrNull { isSpringMappingAnnotation(it.qualifiedName) }
    }

    private suspend fun getContainingClassAnnotation(method: PsiMethod): PsiAnnotation? = readAction {
        method.containingClass?.modifierList?.annotations?.firstOrNull { isSpringMappingAnnotation(it.qualifiedName) }
    }

    private suspend fun getPath(annotation: PsiAnnotation?): String? = readAction {
        getAnnotationParamValue(annotation?.findAttributeValue(PATH))
        ?: getAnnotationParamValue(annotation?.findAttributeValue(VALUE))
    }

    private suspend fun getHttpMethod(annotation: PsiAnnotation): String = readAction {
        SPRING_MAPPINGS[annotation.qualifiedName]
        ?: getAnnotationParamValue(annotation.findAttributeValue(METHOD), true)?.firstOrNull()?.uppercase()
        ?: DEFAULT_METHOD
    }

    private fun getAnnotationParamValue(value: PsiAnnotationMemberValue?, isEnum: Boolean = false): String? =
        when (value) {
            is PsiLiteralExpression -> value.value as? String
            is PsiReferenceExpression -> if (isEnum) value.referenceName else null
            is PsiArrayInitializerMemberValue -> value.initializers.firstOrNull()?.let {
                getAnnotationParamValue(it, isEnum)
            }

            else -> null
        }

    private fun combinePaths(classPath: String, methodPath: String): String {
        val base = classPath.trimStart('/').trimEnd('/')
        val sub = methodPath.trimStart('/').trimEnd('/')
        return when {
            base.isEmpty() && sub.isEmpty() -> "/"
            base.isEmpty() -> "/$sub"
            sub.isEmpty() -> "/$base"
            else -> "/$base/$sub"
        }
    }

    private fun convertSpringPlaceholdersToRegex(path: String) =
        path.replace(Regex("\\{[^/]+}"), "[^/]+")

    fun isSpringMappingAnnotation(fqn: String?): Boolean = fqn in SPRING_MAPPINGS.keys
}
