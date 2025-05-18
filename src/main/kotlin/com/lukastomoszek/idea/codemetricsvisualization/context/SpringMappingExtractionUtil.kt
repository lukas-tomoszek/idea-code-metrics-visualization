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
        val methodAnnotation = element as? PsiAnnotation ?: getMethodAnnotation(method) ?: return null to null
        if (!isSpringMappingAnnotation(readAction { methodAnnotation.qualifiedName })) return null to null

        val classAnnotation = getContainingClassAnnotation(method)
        val classPath = getPath(classAnnotation).orEmpty()
        val methodPath = getPath(methodAnnotation).orEmpty()
        val combined = combinePaths(classPath, methodPath)

        val methodType = getHttpMethod(methodAnnotation, classAnnotation)
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

    private suspend fun getHttpMethod(methodAnnotation: PsiAnnotation, classAnnotation: PsiAnnotation?): String =
        readAction {
            SPRING_MAPPINGS[methodAnnotation.qualifiedName]
            ?: getAnnotationParamValue(methodAnnotation.findAttributeValue(METHOD), true)?.uppercase()
            ?: SPRING_MAPPINGS[classAnnotation?.qualifiedName]
            ?: getAnnotationParamValue(classAnnotation?.findAttributeValue(METHOD), true)?.uppercase()
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
        return when {
            classPath.isEmpty() -> "/${methodPath.trimStart('/')}"
            methodPath.isEmpty() -> "/${classPath.trimStart('/')}"
            else -> "/${classPath.trimStart('/').trimEnd('/')}/${methodPath.trimStart('/')}"
        }
    }

    private fun convertSpringPlaceholdersToRegex(path: String) =
        path.replace(Regex("\\{[^/]+}"), "[^/]+")

    fun isSpringMappingAnnotation(fqn: String?): Boolean = fqn in SPRING_MAPPINGS.keys
}
