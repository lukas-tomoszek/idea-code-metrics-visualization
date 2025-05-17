package com.lukastomoszek.idea.codemetricsvisualization.config.persistence

import com.intellij.testFramework.LightPlatformTestCase
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ChartConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ChartSettingsState

class ChartSettingsPersistenceTest : LightPlatformTestCase() {

    private lateinit var settings: ChartSettings

    override fun setUp() {
        super.setUp()
        settings = ChartSettings.getInstance(project)
        settings.update(emptyList())
    }

    fun testDefaultSettingsState() {
        assertNotNull(settings)
        assertEquals("Default settings should be an empty ChartSettingsState", ChartSettingsState(), settings.state)
        assertTrue("Default configs list should be empty", settings.state.configs.isEmpty())
    }

    fun testUpdateAndRetrieveSettings() {
        val initialConfigs = settings.state.configs.toList()
        assertTrue("Initial configs should be empty", initialConfigs.isEmpty())

        val newChartConfig1 = ChartConfig(name = "Test Chart 1", sqlTemplate = "SELECT 1")
        val newChartConfig2 = ChartConfig(name = "Test Chart 2", sqlTemplate = "SELECT 2")
        val updatedConfigs = listOf(newChartConfig1, newChartConfig2)

        settings.update(updatedConfigs)

        val currentState = settings.state
        assertEquals("Number of configs should match updated list", updatedConfigs.size, currentState.configs.size)
        assertEquals("First config should match", newChartConfig1, currentState.configs[0])
        assertEquals("Second config should match", newChartConfig2, currentState.configs[1])

        val retrievedSettingsAgain = ChartSettings.getInstance(project)
        assertSame(
            "Should retrieve the same instance within the same project context",
            settings,
            retrievedSettingsAgain
        )
        assertEquals("Retrieved settings should have the updated state", currentState, retrievedSettingsAgain.state)
    }

    fun testUpdateWithEmptyListClearsSettings() {
        val initialConfig = ChartConfig(name = "Initial Chart")
        settings.update(listOf(initialConfig))
        assertFalse("Settings should not be empty after initial update", settings.state.configs.isEmpty())

        settings.update(emptyList())
        assertTrue("Settings should be empty after updating with an empty list", settings.state.configs.isEmpty())
    }
}
