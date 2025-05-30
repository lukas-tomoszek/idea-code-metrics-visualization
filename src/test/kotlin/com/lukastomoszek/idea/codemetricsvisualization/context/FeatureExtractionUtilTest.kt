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

import com.lukastomoszek.idea.codemetricsvisualization.testutils.FeatureEvaluatorTestConfigs.enumEvaluator
import com.lukastomoszek.idea.codemetricsvisualization.testutils.FeatureEvaluatorTestConfigs.secondParamEnumEvaluator
import com.lukastomoszek.idea.codemetricsvisualization.testutils.FeatureEvaluatorTestConfigs.secondParamStringEvaluator
import com.lukastomoszek.idea.codemetricsvisualization.testutils.FeatureEvaluatorTestConfigs.stringEvaluator
import com.lukastomoszek.idea.codemetricsvisualization.testutils.setupFeatureEvaluatorSettings
import kotlinx.coroutines.runBlocking

class FeatureExtractionUtilTest : BaseContextPsiTest() {
    override fun getTestFilePath(): String = "psi/feature/FeatureExtractionTestData.java"

    fun testGetStringFeatureName() = runBlocking {
        setupFeatureEvaluatorSettings(project, listOf(stringEvaluator))
        val element = loadElement("/* USAGE_STRING_A_MARKER */")
        assertEquals("feature-A", FeatureExtractionUtil.getFeatureName(element))
    }

    fun testGetStringFeatureNameFromSecondParameter() = runBlocking {
        setupFeatureEvaluatorSettings(project, listOf(secondParamStringEvaluator))
        val element = loadElement("/* USAGE_STRING_C_MARKER */")
        assertEquals("feature-C", FeatureExtractionUtil.getFeatureName(element))
    }

    fun testGetEnumFeatureName() = runBlocking {
        setupFeatureEvaluatorSettings(project, listOf(enumEvaluator))
        val element = loadElement("/* USAGE_ENUM_ONE_MARKER */")
        assertEquals("FEATURE_ONE", FeatureExtractionUtil.getFeatureName(element))
    }

    fun testGetEnumFeatureNameFromSecondParameter() = runBlocking {
        setupFeatureEvaluatorSettings(project, listOf(secondParamEnumEvaluator))
        val element = loadElement("/* USAGE_ENUM_DISABLED_MARKER */")
        assertEquals("DISABLED_FEATURE", FeatureExtractionUtil.getFeatureName(element))
    }

    fun testNoFeatureNameWhenNoMatchingConfig() = runBlocking {
        setupFeatureEvaluatorSettings(project, emptyList())
        val element = loadElement("/* USAGE_STRING_A_MARKER */")
        assertNull(FeatureExtractionUtil.getFeatureName(element))
    }

    fun testNoFeatureNameWhenConfigFqnMismatch() = runBlocking {
        setupFeatureEvaluatorSettings(
            project,
            listOf(stringEvaluator.copy(evaluatorMethodFqn = "com.example.features.DifferentClient.getBooleanValue"))
        )
        val element = loadElement("/* USAGE_STRING_A_MARKER */")
        assertNull(FeatureExtractionUtil.getFeatureName(element))
    }

    fun testNoFeatureNameWhenIndexOutOfBounds() = runBlocking {
        setupFeatureEvaluatorSettings(project, listOf(stringEvaluator.copy(featureParameterIndex = 5)))
        val element = loadElement("/* USAGE_STRING_A_MARKER */")
        assertNull(FeatureExtractionUtil.getFeatureName(element))
    }

    fun testNoFeatureNameWhenStringArgumentIsNotLiteral() = runBlocking {
        setupFeatureEvaluatorSettings(project, listOf(stringEvaluator))
        val element = loadElement("/* USAGE_STRING_NON_LITERAL_MARKER */")
        assertNull(FeatureExtractionUtil.getFeatureName(element))
    }

    fun testNoFeatureNameWhenEnumArgumentIsNotReference() = runBlocking {
        setupFeatureEvaluatorSettings(project, listOf(enumEvaluator))
        val element = loadElement("/* USAGE_ENUM_NULL_ARG_MARKER */")
        assertNull(FeatureExtractionUtil.getFeatureName(element))
    }

    fun testGetFeatureNameOnMethodDeclarationItself() = runBlocking {
        setupFeatureEvaluatorSettings(project, listOf(stringEvaluator))
        val element = loadElement("/* METHOD_DECLARATION_MARKER */")
        assertNull(FeatureExtractionUtil.getFeatureName(element))
    }

    fun testGetFeatureNameFromDifferentUsages() = runBlocking {
        setupFeatureEvaluatorSettings(project, listOf(stringEvaluator, enumEvaluator))

        val elementA = loadElement("/* USAGE_STRING_A_MARKER */")
        val elementB = loadElement("/* USAGE_STRING_B_MARKER */")
        val elementEnumOne = loadElement("/* USAGE_ENUM_ONE_MARKER */")
        val elementEnumAnother = loadElement("/* USAGE_ENUM_ANOTHER_MARKER */")

        assertEquals("feature-A", FeatureExtractionUtil.getFeatureName(elementA))
        assertEquals("feature-B", FeatureExtractionUtil.getFeatureName(elementB))
        assertEquals("FEATURE_ONE", FeatureExtractionUtil.getFeatureName(elementEnumOne))
        assertEquals("ANOTHER_FEATURE", FeatureExtractionUtil.getFeatureName(elementEnumAnother))
    }

}
