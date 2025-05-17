package com.lukastomoszek.idea.codemetricsvisualization.context

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.lukastomoszek.idea.codemetricsvisualization.util.TEST_DATA_PATH
import kotlinx.coroutines.runBlocking

private const val TEST_FILE_PATH = "psi/SpringMappingTestData.java"

class SpringMappingExtractionUtilTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = TEST_DATA_PATH

    private fun getAnnotationElementAtMarker(psiFile: PsiFile, marker: String): PsiAnnotation {
        val text = psiFile.text
        val offset = text.indexOf(marker)
        assertTrue("Marker '$marker' not found in file ${psiFile.name}", offset != -1)

        val commentElement = psiFile.findElementAt(offset)!!
        val method = PsiTreeUtil.getParentOfType(commentElement, com.intellij.psi.PsiMethod::class.java)
        assertNotNull("Could not find PsiMethod near marker '$marker'", method)

        val annotations = method!!.modifierList.annotations
        assertTrue("No annotations found on method near marker '$marker'", annotations.isNotEmpty())

        return annotations.first() // or filter by qualified name
    }

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
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// GET_SIMPLE_MARKER")
        assertPathAndMethod(annotation, "/api/v1/base/simpleGet", "GET")
    }

    fun testPostWithValueAttribute() {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// POST_WITH_VALUE_MARKER")
        assertPathAndMethod(annotation, "/api/v1/base/postWithValue", "POST")
    }

    fun testPutWithPathAttributeAndPlaceholder() {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// PUT_WITH_PATH_MARKER")
        assertPathAndMethod(annotation, "/api/v1/base/putWithPath/[^/]+", "PUT")
    }

    fun testDeletePath() {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// DELETE_NO_CLASS_MAPPING_MARKER")
        assertPathAndMethod(annotation, "/api/v1/base/deletePath", "DELETE")
    }

    fun testRequestMappingGet() {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// REQUEST_MAPPING_GET_MARKER")
        assertPathAndMethod(annotation, "/api/v1/base/requestMappingGet", "GET")
    }

    fun testRequestMappingPostArray() {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// REQUEST_MAPPING_POST_ARRAY_MARKER")
        assertPathAndMethod(annotation, "/api/v1/base/requestMappingPostArray1", "POST")
    }

    fun testRequestMappingNoMethodSpecified() {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// REQUEST_MAPPING_NO_METHOD_MARKER")
        assertPathAndMethod(annotation, "/api/v1/base/requestMappingNoMethod", "GET")
    }

    fun testPatchWithMultiplePlaceholders() {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// PATCH_WITH_PLACEHOLDER_MARKER")
        assertPathAndMethod(annotation, "/api/v1/base/patch/[^/]+/item/[^/]+", "PATCH")
    }

    fun testGetMappingWithRootPath() {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// GET_ROOT_PATH_MARKER")
        assertPathAndMethod(annotation, "/api/v1/base/", "GET")
    }

    fun testGetMappingWithEmptyPath() {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// GET_EMPTY_PATH_MARKER")
        assertPathAndMethod(annotation, "/api/v1/base", "GET")
    }

    fun testGetMappingOnRootPathController() {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// GET_MAPPING_ON_ROOT_CONTROLLER_MARKER")
        assertPathAndMethod(annotation, "/specific", "GET")
    }

    fun testGetMappingEmptyOnRootPathController() {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// GET_MAPPING_EMPTY_ON_ROOT_CONTROLLER_MARKER")
        assertPathAndMethod(annotation, "/", "GET")
    }

    fun testGetNoBaseMapping() {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// GET_NO_BASE_MARKER")
        assertPathAndMethod(annotation, "/noBase", "GET")
    }

    fun testPostNoBaseMappingWithValueAttribute() {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// POST_NO_BASE_VALUE_ATTRIBUTE_MARKER")
        assertPathAndMethod(annotation, "/noBasePost", "POST")
    }

    fun testNonSpringAnnotation() {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// NOT_SPRING_MARKER")
        assertPathAndMethod(annotation, null, null)
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

    fun testClassLevelDefaultMethod() {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// CLASS_LEVEL_DEFAULT_METHOD_MARKER")
        assertPathAndMethod(annotation, "/pathOnly", "PUT")
    }

    fun testClassLevelOverriddenMethod() {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// CLASS_LEVEL_OVERRIDDEN_METHOD_MARKER")
        assertPathAndMethod(annotation, "/overrideMethod", "POST")
    }

    fun testClassLevelPathOnly() {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// CLASS_LEVEL_PATH_ONLY_MARKER")
        assertPathAndMethod(annotation, "/prefixOnly", "GET")
    }

    fun testClassLevelValueOnly() {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val annotation = getAnnotationElementAtMarker(psiFile, "// CLASS_LEVEL_VALUE_ONLY_MARKER")
        assertPathAndMethod(annotation, "/valueOnly", "GET")
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
}
