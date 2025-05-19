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

import com.lukastomoszek.idea.codemetricsvisualization.testutils.FeatureEvaluatorTestConfigs
import com.lukastomoszek.idea.codemetricsvisualization.testutils.getElementAtMarker
import com.lukastomoszek.idea.codemetricsvisualization.testutils.setupFeatureEvaluatorSettings
import kotlinx.coroutines.runBlocking

class PsiContextResolverTest : BaseContextPsiTest() {
    override fun getTestFilePath(): String = "psi/context/PsiContextResolverTestData.java"

    private val expectedMethods = listOf(
        "com.example.contextresolver.AnotherClassInFile.anotherMethod",
        "com.example.contextresolver.PsiContextResolverTestData.methodOne",
        "com.example.contextresolver.PsiContextResolverTestData.methodThree",
        "com.example.contextresolver.PsiContextResolverTestData.methodTwo",
        "com.example.contextresolver.PsiContextResolverTestData.utilityMethod"
    ).sorted()

    private val expectedFeatures = listOf("feature.one", "ANOTHER_FEATURE").sorted()

    private val expectedMappingPaths = listOf(
        "/api/context",
        "/api/context/methodOne",
        "/api/context/methodTwo/[^/]+"
    ).sorted()

    private val expectedMappingMethods = listOf("GET", "POST").sorted()

    override fun setUp() {
        super.setUp()
        setupFeatureEvaluatorSettings(
            project, listOf(
                FeatureEvaluatorTestConfigs.stringEvaluator,
                FeatureEvaluatorTestConfigs.enumEvaluator
            )
        )
    }

    fun testContextInfoInsideMethodWithFeatureAndMapping() = runBlocking {
        val element = loadElement("/* METHOD_ONE_BODY_MARKER */")
        val contextInfo = PsiContextResolver.getContextInfoFromPsi(element)

        assertEquals("com.example.contextresolver.PsiContextResolverTestData.methodOne", contextInfo.methodFqn)
        assertNull(contextInfo.featureName)
        assertEquals(expectedMethods, contextInfo.allMethodsInFile.sorted())
        assertEquals(expectedFeatures, contextInfo.allFeaturesInFile.sorted())
        assertEquals("/api/context/methodOne", contextInfo.mappingPath)
        assertEquals("GET", contextInfo.mappingMethod)
        assertEquals(expectedMappingPaths, contextInfo.allMappingPathsInFile.sorted())
        assertEquals(expectedMappingMethods, contextInfo.allMappingMethodsInFile.sorted())
    }

    fun testContextInfoAtFeatureCall() = runBlocking {
        val element = loadElement("/* FEATURE_ONE_CALL_MARKER */")
        val contextInfo = PsiContextResolver.getContextInfoFromPsi(element)

        assertEquals("com.example.contextresolver.PsiContextResolverTestData.methodOne", contextInfo.methodFqn)
        assertEquals("feature.one", contextInfo.featureName)
        assertEquals(expectedMethods, contextInfo.allMethodsInFile.sorted())
        assertEquals(expectedFeatures, contextInfo.allFeaturesInFile.sorted())
        assertEquals("/api/context/methodOne", contextInfo.mappingPath)
        assertEquals("GET", contextInfo.mappingMethod)
        assertEquals(expectedMappingPaths, contextInfo.allMappingPathsInFile.sorted())
        assertEquals(expectedMappingMethods, contextInfo.allMappingMethodsInFile.sorted())
    }

    fun testContextInfoAtSpringAnnotationMethodLevel() = runBlocking {
        val element = loadElement("/* METHOD_ONE_ANNOTATION_MARKER */")
        val contextInfo = PsiContextResolver.getContextInfoFromPsi(element)

        assertEquals("com.example.contextresolver.PsiContextResolverTestData.methodOne", contextInfo.methodFqn)
        assertNull(contextInfo.featureName)
        assertEquals(expectedMethods, contextInfo.allMethodsInFile.sorted())
        assertEquals(expectedFeatures, contextInfo.allFeaturesInFile.sorted())
        assertEquals("/api/context/methodOne", contextInfo.mappingPath)
        assertEquals("GET", contextInfo.mappingMethod)
        assertEquals(expectedMappingPaths, contextInfo.allMappingPathsInFile.sorted())
        assertEquals(expectedMappingMethods, contextInfo.allMappingMethodsInFile.sorted())
    }

    fun testContextInfoInsideSimpleMethodNoFeaturesOrDirectMapping() = runBlocking {
        val element = loadElement("/* UTILITY_METHOD_BODY_MARKER */")
        val contextInfo = PsiContextResolver.getContextInfoFromPsi(element)

        assertEquals("com.example.contextresolver.PsiContextResolverTestData.utilityMethod", contextInfo.methodFqn)
        assertNull(contextInfo.featureName)
        assertNull(contextInfo.mappingPath)
        assertNull(contextInfo.mappingMethod)
        assertEquals(expectedMethods, contextInfo.allMethodsInFile.sorted())
        assertEquals(expectedFeatures, contextInfo.allFeaturesInFile.sorted())
        assertEquals(expectedMappingPaths, contextInfo.allMappingPathsInFile.sorted())
        assertEquals(expectedMappingMethods, contextInfo.allMappingMethodsInFile.sorted())
    }

    fun testContextInfoClassLevelFieldElement() = runBlocking {
        val element = loadElement("/* CLASS_LEVEL_FIELD_MARKER */")
        val contextInfo = PsiContextResolver.getContextInfoFromPsi(element)

        assertNull(contextInfo.methodFqn)
        assertNull(contextInfo.featureName)
        assertNull(contextInfo.mappingPath)
        assertNull(contextInfo.mappingMethod)
        assertEquals(expectedMethods, contextInfo.allMethodsInFile.sorted())
        assertEquals(expectedFeatures, contextInfo.allFeaturesInFile.sorted())
        assertEquals(expectedMappingPaths, contextInfo.allMappingPathsInFile.sorted())
        assertEquals(expectedMappingMethods, contextInfo.allMappingMethodsInFile.sorted())
    }

    fun testContextInfoForMethodWithNoPathInAnnotationInheritsClassPath() = runBlocking {
        val element = loadElement("/* METHOD_THREE_BODY_MARKER */")
        val contextInfo = PsiContextResolver.getContextInfoFromPsi(element)

        assertEquals("com.example.contextresolver.PsiContextResolverTestData.methodThree", contextInfo.methodFqn)
        assertNull(contextInfo.featureName)
        assertEquals("/api/context", contextInfo.mappingPath)
        assertEquals("GET", contextInfo.mappingMethod)
        assertEquals(expectedMethods, contextInfo.allMethodsInFile.sorted())
    }

    fun testContextInfoNoFeatureEvaluatorsConfigured() = runBlocking {
        clearProjectSettings()

        val element = loadElement("/* FEATURE_ONE_CALL_MARKER */")
        val contextInfo = PsiContextResolver.getContextInfoFromPsi(element)

        assertEquals("com.example.contextresolver.PsiContextResolverTestData.methodOne", contextInfo.methodFqn)
        assertNull(contextInfo.featureName)
        assertTrue(contextInfo.allFeaturesInFile.isEmpty())
        assertEquals("/api/context/methodOne", contextInfo.mappingPath)
        assertEquals("GET", contextInfo.mappingMethod)
        assertEquals(expectedMethods, contextInfo.allMethodsInFile.sorted())
    }

    fun testContextInfoFileWithNoSpringAnnotations() = runBlocking {
        val tempFilePath = "NoSpring.java"
        myFixture.configureByText(
            tempFilePath, """
            package com.example.contextresolver;
            import com.example.features.FeatureClient;
            import com.example.features.FeatureKey;

            public class NoSpring {
                private FeatureClient fc = new FeatureClient();
                public void myMethod() { /* METHOD_MARKER */
                    fc.getBooleanValue("my.feature" /* FEATURE_MARKER */, false);
                }
            }
        """.trimIndent()
        )
        val element = myFixture.file.getElementAtMarker("/* METHOD_MARKER */")
        val contextInfo = PsiContextResolver.getContextInfoFromPsi(element)

        assertEquals("com.example.contextresolver.NoSpring.myMethod", contextInfo.methodFqn)
        assertNull(contextInfo.featureName)
        assertNull(contextInfo.mappingPath)
        assertNull(contextInfo.mappingMethod)
        assertEquals(listOf("com.example.contextresolver.NoSpring.myMethod"), contextInfo.allMethodsInFile)
        assertEquals(listOf("my.feature"), contextInfo.allFeaturesInFile)
        assertTrue(contextInfo.allMappingPathsInFile.isEmpty())
        assertTrue(contextInfo.allMappingMethodsInFile.isEmpty())

        val elementAtFeature = myFixture.file.getElementAtMarker("/* FEATURE_MARKER */")
        val contextInfoAtFeature = PsiContextResolver.getContextInfoFromPsi(elementAtFeature)
        assertEquals("my.feature", contextInfoAtFeature.featureName)
    }

    fun testContextInfoEmptyJavaFile() = runBlocking {
        val element = loadElement("psi/util/EmptyClass.java", "/* EMPTY_CLASS_MARKER */")
        val contextInfo = PsiContextResolver.getContextInfoFromPsi(element)

        assertNull(contextInfo.methodFqn)
        assertNull(contextInfo.featureName)
        assertTrue(contextInfo.allMethodsInFile.isEmpty())
        assertTrue(contextInfo.allFeaturesInFile.isEmpty())
        assertNull(contextInfo.mappingPath)
        assertNull(contextInfo.mappingMethod)
        assertTrue(contextInfo.allMappingPathsInFile.isEmpty())
        assertTrue(contextInfo.allMappingMethodsInFile.isEmpty())
    }

    fun testContextInfoClassDeclarationElement() = runBlocking {
        val element = loadElement("/* CLASS_DECLARATION_MARKER */")
        val contextInfo = PsiContextResolver.getContextInfoFromPsi(element)

        assertNull(contextInfo.methodFqn)
        assertNull(contextInfo.featureName)
        assertNull(contextInfo.mappingPath)
        assertNull(contextInfo.mappingMethod)
        assertEquals(expectedMethods, contextInfo.allMethodsInFile.sorted())
        assertEquals(expectedFeatures, contextInfo.allFeaturesInFile.sorted())
        assertEquals(expectedMappingPaths, contextInfo.allMappingPathsInFile.sorted())
    }
}
