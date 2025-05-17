package com.lukastomoszek.idea.codemetricsvisualization.context

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.FeatureEvaluatorSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.FeatureEvaluatorConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.FeatureParameterType
import com.lukastomoszek.idea.codemetricsvisualization.util.TEST_DATA_PATH
import kotlinx.coroutines.runBlocking

private const val TEST_FILE_PATH = "psi/FeatureExtractionTestData.java"

class FeatureExtractionUtilTest : BasePlatformTestCase() {

    override fun getTestDataPath() = TEST_DATA_PATH

    private fun getElementAtMarker(psiFile: PsiFile, marker: String): PsiElement {
        val text = psiFile.text
        val index = text.indexOf(marker)
        assertTrue("Marker '$marker' not found in file ${psiFile.name}", index != -1)
        val offset = index + marker.length
        val elementAtMarker = psiFile.findElementAt(offset)
        assertNotNull("Element at marker '$marker' should not be null", elementAtMarker)
        return elementAtMarker!!
    }

    private fun setupFeatureEvaluatorSettings(configs: List<FeatureEvaluatorConfig>) {
        val settings = FeatureEvaluatorSettings.getInstance(project)
        settings.update(configs)
    }

    override fun tearDown() {
        try {
            FeatureEvaluatorSettings.getInstance(project).update(emptyList())
        } finally {
            super.tearDown()
        }
    }

    fun testGetStringFeatureName() = runBlocking {
        setupFeatureEvaluatorSettings(
            listOf(
                FeatureEvaluatorConfig(
                    name = "String Evaluator",
                    evaluatorMethodFqn = "com.example.features.FeatureClient.getBooleanValue",
                    featureParameterIndex = 0,
                    featureParameterType = FeatureParameterType.STRING
                )
            )
        )
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val element = getElementAtMarker(psiFile, "client.getBooleanValue(\"feature-A\"")
        val featureName = FeatureExtractionUtil.getFeatureName(element)
        assertEquals("feature-A", featureName)
    }

    fun testGetStringFeatureNameFromSecondParameter() = runBlocking {
        setupFeatureEvaluatorSettings(
            listOf(
                FeatureEvaluatorConfig(
                    name = "String Evaluator Second Param",
                    evaluatorMethodFqn = "com.example.features.FeatureClient.getStringValue",
                    featureParameterIndex = 1,
                    featureParameterType = FeatureParameterType.STRING
                )
            )
        )
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val element = getElementAtMarker(psiFile, "client.getStringValue(0, \"feature-C\"")
        val featureName = FeatureExtractionUtil.getFeatureName(element)
        assertEquals("feature-C", featureName)
    }

    fun testGetEnumFeatureName() = runBlocking {
        setupFeatureEvaluatorSettings(
            listOf(
                FeatureEvaluatorConfig(
                    name = "Enum Evaluator",
                    evaluatorMethodFqn = "com.example.features.FeatureClient.isEnabled",
                    featureParameterIndex = 0,
                    featureParameterType = FeatureParameterType.ENUM_CONSTANT
                )
            )
        )
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val element = getElementAtMarker(psiFile, "client.isEnabled(FeatureKey.FEATURE_ONE")
        val featureName = FeatureExtractionUtil.getFeatureName(element)
        assertEquals("FEATURE_ONE", featureName)
    }

    fun testGetEnumFeatureNameFromSecondParameter() = runBlocking {
        setupFeatureEvaluatorSettings(
            listOf(
                FeatureEvaluatorConfig(
                    name = "Enum Evaluator Second Param",
                    evaluatorMethodFqn = "com.example.features.FeatureClient.getIntValue",
                    featureParameterIndex = 1,
                    featureParameterType = FeatureParameterType.ENUM_CONSTANT
                )
            )
        )
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val element = getElementAtMarker(psiFile, "client.getIntValue(0, FeatureKey.DISABLED_FEATURE")
        val featureName = FeatureExtractionUtil.getFeatureName(element)
        assertEquals("DISABLED_FEATURE", featureName)
    }

    fun testNoFeatureNameWhenNoMatchingConfig() = runBlocking {
        setupFeatureEvaluatorSettings(emptyList())
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val element = getElementAtMarker(psiFile, "client.getBooleanValue(\"feature-A\"")
        val featureName = FeatureExtractionUtil.getFeatureName(element)
        assertNull(featureName)
    }

    fun testNoFeatureNameWhenConfigFqnMismatch() = runBlocking {
        setupFeatureEvaluatorSettings(
            listOf(
                FeatureEvaluatorConfig(
                    name = "Wrong FQN Config",
                    evaluatorMethodFqn = "com.example.features.DifferentClient.getBooleanValue",
                    featureParameterIndex = 0,
                    featureParameterType = FeatureParameterType.STRING
                )
            )
        )
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val element = getElementAtMarker(psiFile, "client.getBooleanValue(\"feature-A\"")
        val featureName = FeatureExtractionUtil.getFeatureName(element)
        assertNull(featureName)
    }

    fun testNoFeatureNameWhenIndexOutOfBounds() = runBlocking {
        setupFeatureEvaluatorSettings(
            listOf(
                FeatureEvaluatorConfig(
                    name = "Index OOB Config",
                    evaluatorMethodFqn = "com.example.features.FeatureClient.getBooleanValue",
                    featureParameterIndex = 5,
                    featureParameterType = FeatureParameterType.STRING
                )
            )
        )
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val element = getElementAtMarker(psiFile, "client.getBooleanValue(\"feature-A\"")
        val featureName = FeatureExtractionUtil.getFeatureName(element)
        assertNull(featureName)
    }

    fun testNoFeatureNameWhenStringArgumentIsNotLiteral() = runBlocking {
        setupFeatureEvaluatorSettings(
            listOf(
                FeatureEvaluatorConfig(
                    name = "String Evaluator",
                    evaluatorMethodFqn = "com.example.features.FeatureClient.getBooleanValue",
                    featureParameterIndex = 0,
                    featureParameterType = FeatureParameterType.STRING
                )
            )
        )
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val element = getElementAtMarker(psiFile, "client.getBooleanValue(System.getenv(\"RUNTIME_KEY\")")
        val featureName = FeatureExtractionUtil.getFeatureName(element)
        assertNull(featureName)
    }

    fun testNoFeatureNameWhenEnumArgumentIsNotReference() = runBlocking {
        setupFeatureEvaluatorSettings(
            listOf(
                FeatureEvaluatorConfig(
                    name = "Enum Evaluator",
                    evaluatorMethodFqn = "com.example.features.FeatureClient.isEnabled",
                    featureParameterIndex = 0,
                    featureParameterType = FeatureParameterType.ENUM_CONSTANT
                )
            )
        )
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val element = getElementAtMarker(psiFile, "client.isEnabled(null")
        val featureName = FeatureExtractionUtil.getFeatureName(element)
        assertNull(featureName)
    }

    fun testGetFeatureNameOnMethodDeclarationItself() = runBlocking {
        setupFeatureEvaluatorSettings(
            listOf(
                FeatureEvaluatorConfig(
                    name = "String Evaluator",
                    evaluatorMethodFqn = "com.example.features.FeatureClient.getBooleanValue",
                    featureParameterIndex = 0,
                    featureParameterType = FeatureParameterType.STRING
                )
            )
        )
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val element = getElementAtMarker(psiFile, "public boolean getBooleanValue(String key")
        val featureName = FeatureExtractionUtil.getFeatureName(element)
        assertNull("Should be null when element is the method declaration, not a call", featureName)
    }

    fun testGetFeatureNameFromDifferentUsages() = runBlocking {
        setupFeatureEvaluatorSettings(
            listOf(
                FeatureEvaluatorConfig(
                    name = "String Evaluator",
                    evaluatorMethodFqn = "com.example.features.FeatureClient.getBooleanValue",
                    featureParameterIndex = 0,
                    featureParameterType = FeatureParameterType.STRING
                ),
                FeatureEvaluatorConfig(
                    name = "Enum Evaluator",
                    evaluatorMethodFqn = "com.example.features.FeatureClient.isEnabled",
                    featureParameterIndex = 0,
                    featureParameterType = FeatureParameterType.ENUM_CONSTANT
                )
            )
        )
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)

        var element = getElementAtMarker(psiFile, "client.getBooleanValue(\"feature-A\"")
        assertEquals("feature-A", FeatureExtractionUtil.getFeatureName(element))

        element = getElementAtMarker(psiFile, "client.getBooleanValue(\"feature-B\"")
        assertEquals("feature-B", FeatureExtractionUtil.getFeatureName(element))

        element = getElementAtMarker(psiFile, "client.isEnabled(FeatureKey.FEATURE_ONE")
        assertEquals("FEATURE_ONE", FeatureExtractionUtil.getFeatureName(element))

        element = getElementAtMarker(psiFile, "client.isEnabled(FeatureKey.ANOTHER_FEATURE")
        assertEquals("ANOTHER_FEATURE", FeatureExtractionUtil.getFeatureName(element))
    }
}
