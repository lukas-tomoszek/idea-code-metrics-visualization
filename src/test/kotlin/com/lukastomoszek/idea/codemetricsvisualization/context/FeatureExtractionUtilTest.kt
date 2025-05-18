package com.lukastomoszek.idea.codemetricsvisualization.context

import com.lukastomoszek.idea.codemetricsvisualization.testutils.FeatureEvaluatorTestConfigs.enumEvaluator
import com.lukastomoszek.idea.codemetricsvisualization.testutils.FeatureEvaluatorTestConfigs.secondParamEnumEvaluator
import com.lukastomoszek.idea.codemetricsvisualization.testutils.FeatureEvaluatorTestConfigs.secondParamStringEvaluator
import com.lukastomoszek.idea.codemetricsvisualization.testutils.FeatureEvaluatorTestConfigs.stringEvaluator
import com.lukastomoszek.idea.codemetricsvisualization.testutils.setupFeatureEvaluatorSettings
import kotlinx.coroutines.runBlocking

private const val TEST_FILE_PATH = "psi/feature/FeatureExtractionTestData.java"

class FeatureExtractionUtilTest : BaseContextPsiTest() {

    fun testGetStringFeatureName() = runBlocking {
        setupFeatureEvaluatorSettings(project, listOf(stringEvaluator))
        val element = loadElement(TEST_FILE_PATH, "/* USAGE_STRING_A_MARKER */")
        assertEquals("feature-A", FeatureExtractionUtil.getFeatureName(element))
    }

    fun testGetStringFeatureNameFromSecondParameter() = runBlocking {
        setupFeatureEvaluatorSettings(project, listOf(secondParamStringEvaluator))
        val element = loadElement(TEST_FILE_PATH, "/* USAGE_STRING_C_MARKER */")
        assertEquals("feature-C", FeatureExtractionUtil.getFeatureName(element))
    }

    fun testGetEnumFeatureName() = runBlocking {
        setupFeatureEvaluatorSettings(project, listOf(enumEvaluator))
        val element = loadElement(TEST_FILE_PATH, "/* USAGE_ENUM_ONE_MARKER */")
        assertEquals("FEATURE_ONE", FeatureExtractionUtil.getFeatureName(element))
    }

    fun testGetEnumFeatureNameFromSecondParameter() = runBlocking {
        setupFeatureEvaluatorSettings(project, listOf(secondParamEnumEvaluator))
        val element = loadElement(TEST_FILE_PATH, "/* USAGE_ENUM_DISABLED_MARKER */")
        assertEquals("DISABLED_FEATURE", FeatureExtractionUtil.getFeatureName(element))
    }

    fun testNoFeatureNameWhenNoMatchingConfig() = runBlocking {
        setupFeatureEvaluatorSettings(project, emptyList())
        val element = loadElement(TEST_FILE_PATH, "/* USAGE_STRING_A_MARKER */")
        assertNull(FeatureExtractionUtil.getFeatureName(element))
    }

    fun testNoFeatureNameWhenConfigFqnMismatch() = runBlocking {
        setupFeatureEvaluatorSettings(
            project,
            listOf(stringEvaluator.copy(evaluatorMethodFqn = "com.example.features.DifferentClient.getBooleanValue"))
        )
        val element = loadElement(TEST_FILE_PATH, "/* USAGE_STRING_A_MARKER */")
        assertNull(FeatureExtractionUtil.getFeatureName(element))
    }

    fun testNoFeatureNameWhenIndexOutOfBounds() = runBlocking {
        setupFeatureEvaluatorSettings(project, listOf(stringEvaluator.copy(featureParameterIndex = 5)))
        val element = loadElement(TEST_FILE_PATH, "/* USAGE_STRING_A_MARKER */")
        assertNull(FeatureExtractionUtil.getFeatureName(element))
    }

    fun testNoFeatureNameWhenStringArgumentIsNotLiteral() = runBlocking {
        setupFeatureEvaluatorSettings(project, listOf(stringEvaluator))
        val element = loadElement(TEST_FILE_PATH, "/* USAGE_STRING_NON_LITERAL_MARKER */")
        assertNull(FeatureExtractionUtil.getFeatureName(element))
    }

    fun testNoFeatureNameWhenEnumArgumentIsNotReference() = runBlocking {
        setupFeatureEvaluatorSettings(project, listOf(enumEvaluator))
        val element = loadElement(TEST_FILE_PATH, "/* USAGE_ENUM_NULL_ARG_MARKER */")
        assertNull(FeatureExtractionUtil.getFeatureName(element))
    }

    fun testGetFeatureNameOnMethodDeclarationItself() = runBlocking {
        setupFeatureEvaluatorSettings(project, listOf(stringEvaluator))
        val element = loadElement(TEST_FILE_PATH, "/* METHOD_DECLARATION_MARKER */")
        assertNull(FeatureExtractionUtil.getFeatureName(element))
    }

    fun testGetFeatureNameFromDifferentUsages() = runBlocking {
        setupFeatureEvaluatorSettings(project, listOf(stringEvaluator, enumEvaluator))

        val elementA = loadElement(TEST_FILE_PATH, "/* USAGE_STRING_A_MARKER */")
        val elementB = loadElement(TEST_FILE_PATH, "/* USAGE_STRING_B_MARKER */")
        val elementEnumOne = loadElement(TEST_FILE_PATH, "/* USAGE_ENUM_ONE_MARKER */")
        val elementEnumAnother = loadElement(TEST_FILE_PATH, "/* USAGE_ENUM_ANOTHER_MARKER */")

        assertEquals("feature-A", FeatureExtractionUtil.getFeatureName(elementA))
        assertEquals("feature-B", FeatureExtractionUtil.getFeatureName(elementB))
        assertEquals("FEATURE_ONE", FeatureExtractionUtil.getFeatureName(elementEnumOne))
        assertEquals("ANOTHER_FEATURE", FeatureExtractionUtil.getFeatureName(elementEnumAnother))
    }

}
