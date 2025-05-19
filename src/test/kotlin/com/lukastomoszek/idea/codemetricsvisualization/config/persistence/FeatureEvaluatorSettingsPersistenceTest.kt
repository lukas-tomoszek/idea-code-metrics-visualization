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

package com.lukastomoszek.idea.codemetricsvisualization.config.persistence

import com.intellij.testFramework.LightPlatformTestCase
import com.lukastomoszek.idea.codemetricsvisualization.config.state.FeatureEvaluatorConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.FeatureEvaluatorSettingsState
import com.lukastomoszek.idea.codemetricsvisualization.config.state.FeatureParameterType

class FeatureEvaluatorSettingsPersistenceTest : LightPlatformTestCase() {

    private lateinit var settings: FeatureEvaluatorSettings

    override fun setUp() {
        super.setUp()
        settings = FeatureEvaluatorSettings.getInstance(project)
        settings.update(emptyList())
    }

    fun testDefaultSettingsState() {
        assertNotNull(settings)
        assertEquals(FeatureEvaluatorSettingsState(), settings.state)
        assertTrue(settings.state.configs.isEmpty())
    }

    fun testUpdateAndRetrieveSettings() {
        val initialConfigs = settings.state.configs.toList()
        assertTrue(initialConfigs.isEmpty())

        val newConfig1 = FeatureEvaluatorConfig(
            name = "Evaluator 1",
            evaluatorMethodFqn = "com.example.Eval1.eval",
            featureParameterIndex = 0,
            featureParameterType = FeatureParameterType.STRING
        )
        val newConfig2 = FeatureEvaluatorConfig(
            name = "Evaluator 2",
            evaluatorMethodFqn = "com.example.Eval2.check",
            featureParameterIndex = 1,
            featureParameterType = FeatureParameterType.ENUM_CONSTANT
        )
        val updatedConfigs = listOf(newConfig1, newConfig2)

        settings.update(updatedConfigs)

        val currentState = settings.state
        assertEquals(updatedConfigs.size, currentState.configs.size)
        assertEquals(newConfig1, currentState.configs[0])
        assertEquals(newConfig2, currentState.configs[1])

        val retrievedSettingsAgain = FeatureEvaluatorSettings.getInstance(project)
        assertSame(settings, retrievedSettingsAgain)
        assertEquals(currentState, retrievedSettingsAgain.state)
    }

    fun testUpdateWithEmptyListClearsSettings() {
        val initialConfig = FeatureEvaluatorConfig(name = "Initial Evaluator")
        settings.update(listOf(initialConfig))
        assertFalse(settings.state.configs.isEmpty())

        settings.update(emptyList())
        assertTrue(settings.state.configs.isEmpty())
    }
}
