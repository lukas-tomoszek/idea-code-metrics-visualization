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

import com.intellij.psi.PsiElement
import kotlinx.coroutines.runBlocking

class SpringMappingExtractionUtilTest : BaseContextPsiTest() {
    override fun getTestFilePath(): String = "psi/mapping/SpringMappingTestData.java"

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
        val element = loadElement("/* GET_SIMPLE_MARKER */")
        assertPathAndMethod(element, "/api/v1/base/simpleGet", "GET")
    }

    fun testPostWithValueAttribute() {
        val element = loadElement("/* POST_WITH_VALUE_MARKER */")
        assertPathAndMethod(element, "/api/v1/base/postWithValue", "POST")
    }

    fun testPutWithPathAttributeAndPlaceholder() {
        val element = loadElement("/* PUT_WITH_PATH_MARKER */")
        assertPathAndMethod(element, "/api/v1/base/putWithPath/[^/]+", "PUT")
    }

    fun testDeletePath() {
        val element = loadElement("/* DELETE_NO_CLASS_MAPPING_MARKER */")
        assertPathAndMethod(element, "/api/v1/base/deletePath", "DELETE")
    }

    fun testRequestMappingGet() {
        val element = loadElement("/* REQUEST_MAPPING_GET_MARKER */")
        assertPathAndMethod(element, "/api/v1/base/requestMappingGet", "GET")
    }

    fun testRequestMappingPostArray() {
        val element = loadElement("/* REQUEST_MAPPING_POST_ARRAY_MARKER */")
        assertPathAndMethod(element, "/api/v1/base/requestMappingPostArray1", "POST")
    }

    fun testRequestMappingNoMethodSpecified() {
        val element = loadElement("/* REQUEST_MAPPING_NO_METHOD_MARKER */")
        assertPathAndMethod(element, "/api/v1/base/requestMappingNoMethod", "GET")
    }

    fun testPatchWithMultiplePlaceholders() {
        val element = loadElement("/* PATCH_WITH_PLACEHOLDER_MARKER */")
        assertPathAndMethod(element, "/api/v1/base/patch/[^/]+/item/[^/]+", "PATCH")
    }

    fun testGetMappingWithRootPath() {
        val element = loadElement("/* GET_ROOT_PATH_MARKER */")
        assertPathAndMethod(element, "/api/v1/base/", "GET")
    }

    fun testGetMappingWithEmptyPath() {
        val element = loadElement("/* GET_EMPTY_PATH_MARKER */")
        assertPathAndMethod(element, "/api/v1/base", "GET")
    }

    fun testGetMappingOnRootPathController() {
        val element = loadElement("/* GET_MAPPING_ON_ROOT_CONTROLLER_MARKER */")
        assertPathAndMethod(element, "/specific", "GET")
    }

    fun testGetMappingEmptyOnRootPathController() {
        val element = loadElement("/* GET_MAPPING_EMPTY_ON_ROOT_CONTROLLER_MARKER */")
        assertPathAndMethod(element, "/", "GET")
    }

    fun testGetNoBaseMapping() {
        val element = loadElement("/* GET_NO_BASE_MARKER */")
        assertPathAndMethod(element, "/noBase", "GET")
    }

    fun testPostNoBaseMappingWithValueAttribute() {
        val element = loadElement("/* POST_NO_BASE_VALUE_ATTRIBUTE_MARKER */")
        assertPathAndMethod(element, "/noBasePost", "POST")
    }

    fun testNonSpringAnnotation() {
        val element = loadElement("/* NOT_SPRING_MARKER */")
        assertPathAndMethod(element, null, null)
    }

    fun testClassLevelDefaultMethod() {
        val element = loadElement("/* CLASS_LEVEL_DEFAULT_METHOD_MARKER */")
        assertPathAndMethod(element, "/pathOnly", "PUT")
    }

    fun testClassLevelOverriddenMethod() {
        val element = loadElement("/* CLASS_LEVEL_OVERRIDDEN_METHOD_MARKER */")
        assertPathAndMethod(element, "/overrideMethod", "POST")
    }

    fun testClassLevelPathOnly() {
        val element = loadElement("/* CLASS_LEVEL_PATH_ONLY_MARKER */")
        assertPathAndMethod(element, "/prefixOnly", "GET")
    }

    fun testClassLevelValueOnly() {
        val element = loadElement("/* CLASS_LEVEL_VALUE_ONLY_MARKER */")
        assertPathAndMethod(element, "/valueOnly", "GET")
    }

    fun testExtractPathAndMethodOnMethodIdentifier() = runBlocking {
        val psiFile = myFixture.configureByFile(getTestFilePath())
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
