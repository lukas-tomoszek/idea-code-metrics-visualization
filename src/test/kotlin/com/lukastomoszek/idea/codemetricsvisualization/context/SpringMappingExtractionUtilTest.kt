package com.lukastomoszek.idea.codemetricsvisualization.context

import com.intellij.psi.PsiElement
import kotlinx.coroutines.runBlocking

private const val TEST_FILE_PATH = "psi/mapping/SpringMappingTestData.java"

class SpringMappingExtractionUtilTest : BaseContextPsiTest() {

    private fun assertPathAndMethod(
        element: PsiElement,
        expectedPath: String?,
        expectedMethod: String?
    ) = runBlocking {
        val (path, method) = SpringMappingExtractionUtil.extractPathAndMethod(element)
        assertEquals("Path mismatch", expectedPath, path)
        assertEquals("Method mismatch", expectedMethod, method)
    }

    fun testSimpleGet() {
        val element = loadElement(TEST_FILE_PATH, "/* GET_SIMPLE_MARKER */")
        assertPathAndMethod(element, "/api/v1/base/simpleGet", "GET")
    }

    fun testPostWithValueAttribute() {
        val element = loadElement(TEST_FILE_PATH, "/* POST_WITH_VALUE_MARKER */")
        assertPathAndMethod(element, "/api/v1/base/postWithValue", "POST")
    }

    fun testPutWithPathAttributeAndPlaceholder() {
        val element = loadElement(TEST_FILE_PATH, "/* PUT_WITH_PATH_MARKER */")
        assertPathAndMethod(element, "/api/v1/base/putWithPath/[^/]+", "PUT")
    }

    fun testDeletePath() {
        val element = loadElement(TEST_FILE_PATH, "/* DELETE_NO_CLASS_MAPPING_MARKER */")
        assertPathAndMethod(element, "/api/v1/base/deletePath", "DELETE")
    }

    fun testRequestMappingGet() {
        val element = loadElement(TEST_FILE_PATH, "/* REQUEST_MAPPING_GET_MARKER */")
        assertPathAndMethod(element, "/api/v1/base/requestMappingGet", "GET")
    }

    fun testRequestMappingPostArray() {
        val element = loadElement(TEST_FILE_PATH, "/* REQUEST_MAPPING_POST_ARRAY_MARKER */")
        assertPathAndMethod(element, "/api/v1/base/requestMappingPostArray1", "POST")
    }

    fun testRequestMappingNoMethodSpecified() {
        val element = loadElement(TEST_FILE_PATH, "/* REQUEST_MAPPING_NO_METHOD_MARKER */")
        assertPathAndMethod(element, "/api/v1/base/requestMappingNoMethod", "GET")
    }

    fun testPatchWithMultiplePlaceholders() {
        val element = loadElement(TEST_FILE_PATH, "/* PATCH_WITH_PLACEHOLDER_MARKER */")
        assertPathAndMethod(element, "/api/v1/base/patch/[^/]+/item/[^/]+", "PATCH")
    }

    fun testGetMappingWithRootPath() {
        val element = loadElement(TEST_FILE_PATH, "/* GET_ROOT_PATH_MARKER */")
        assertPathAndMethod(element, "/api/v1/base/", "GET")
    }

    fun testGetMappingWithEmptyPath() {
        val element = loadElement(TEST_FILE_PATH, "/* GET_EMPTY_PATH_MARKER */")
        assertPathAndMethod(element, "/api/v1/base", "GET")
    }

    fun testGetMappingOnRootPathController() {
        val element = loadElement(TEST_FILE_PATH, "/* GET_MAPPING_ON_ROOT_CONTROLLER_MARKER */")
        assertPathAndMethod(element, "/specific", "GET")
    }

    fun testGetMappingEmptyOnRootPathController() {
        val element = loadElement(TEST_FILE_PATH, "/* GET_MAPPING_EMPTY_ON_ROOT_CONTROLLER_MARKER */")
        assertPathAndMethod(element, "/", "GET")
    }

    fun testGetNoBaseMapping() {
        val element = loadElement(TEST_FILE_PATH, "/* GET_NO_BASE_MARKER */")
        assertPathAndMethod(element, "/noBase", "GET")
    }

    fun testPostNoBaseMappingWithValueAttribute() {
        val element = loadElement(TEST_FILE_PATH, "/* POST_NO_BASE_VALUE_ATTRIBUTE_MARKER */")
        assertPathAndMethod(element, "/noBasePost", "POST")
    }

    fun testNonSpringAnnotation() {
        val element = loadElement(TEST_FILE_PATH, "/* NOT_SPRING_MARKER */")
        assertPathAndMethod(element, null, null)
    }

    fun testClassLevelDefaultMethod() {
        val element = loadElement(TEST_FILE_PATH, "/* CLASS_LEVEL_DEFAULT_METHOD_MARKER */")
        assertPathAndMethod(element, "/pathOnly", "PUT")
    }

    fun testClassLevelOverriddenMethod() {
        val element = loadElement(TEST_FILE_PATH, "/* CLASS_LEVEL_OVERRIDDEN_METHOD_MARKER */")
        assertPathAndMethod(element, "/overrideMethod", "POST")
    }

    fun testClassLevelPathOnly() {
        val element = loadElement(TEST_FILE_PATH, "/* CLASS_LEVEL_PATH_ONLY_MARKER */")
        assertPathAndMethod(element, "/prefixOnly", "GET")
    }

    fun testClassLevelValueOnly() {
        val element = loadElement(TEST_FILE_PATH, "/* CLASS_LEVEL_VALUE_ONLY_MARKER */")
        assertPathAndMethod(element, "/valueOnly", "GET")
    }

    fun testExtractPathAndMethodOnMethodIdentifier() = runBlocking {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val marker = "public String simpleGet()"
        val offset = psiFile.text.indexOf(marker)
        val element = psiFile.findElementAt(offset)!!
        val (path, method) = SpringMappingExtractionUtil.extractPathAndMethod(element)
        assertEquals("/api/v1/base/simpleGet", path)
        assertEquals("GET", method)
    }

    fun testIsSpringMappingAnnotation() {
        assertTrue(SpringMappingExtractionUtil.isSpringMappingAnnotation("org.springframework.web.bind.annotation.GetMapping"))
        assertTrue(SpringMappingExtractionUtil.isSpringMappingAnnotation("org.springframework.web.bind.annotation.PostMapping"))
        assertTrue(SpringMappingExtractionUtil.isSpringMappingAnnotation("org.springframework.web.bind.annotation.PutMapping"))
        assertTrue(SpringMappingExtractionUtil.isSpringMappingAnnotation("org.springframework.web.bind.annotation.DeleteMapping"))
        assertTrue(SpringMappingExtractionUtil.isSpringMappingAnnotation("org.springframework.web.bind.annotation.PatchMapping"))
        assertTrue(SpringMappingExtractionUtil.isSpringMappingAnnotation("org.springframework.web.bind.annotation.RequestMapping"))
        assertFalse(SpringMappingExtractionUtil.isSpringMappingAnnotation("com.example.MyCustomAnnotation"))
        assertFalse(SpringMappingExtractionUtil.isSpringMappingAnnotation(null))
        assertFalse(SpringMappingExtractionUtil.isSpringMappingAnnotation(""))
    }
}
