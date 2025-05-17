package com.lukastomoszek.idea.codemetricsvisualization.config.persistence

import com.intellij.testFramework.LightPlatformTestCase
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerSettingsState
import com.lukastomoszek.idea.codemetricsvisualization.linemarker.rule.RangeRule

class LineMarkerSettingsPersistenceTest : LightPlatformTestCase() {

    private lateinit var settings: LineMarkerSettings

    override fun setUp() {
        super.setUp()
        settings = LineMarkerSettings.getInstance(project)
        settings.update(emptyList())
    }

    fun testDefaultSettingsState() {
        assertNotNull(settings)
        assertEquals(LineMarkerSettingsState(), settings.state)
        assertTrue(settings.state.configs.isEmpty())
    }

    fun testUpdateAndRetrieveSettings() {
        val initialConfigs = settings.state.configs.toList()
        assertTrue(initialConfigs.isEmpty())

        val rule1 = RangeRule("0", "10", "#FF0000")
        val rule2 = RangeRule("10", "", "#00FF00")
        val newConfig1 = LineMarkerConfig(
            name = "Marker 1",
            sqlTemplate = "SELECT count(*) FROM t1",
            lineMarkerRules = mutableListOf(rule1)
        )
        val newConfig2 = LineMarkerConfig(
            name = "Marker 2",
            sqlTemplate = "SELECT avg(val) FROM t2",
            lineMarkerRules = mutableListOf(rule2),
            enabled = false
        )
        val updatedConfigs = listOf(newConfig1, newConfig2)

        settings.update(updatedConfigs)

        val currentState = settings.state
        assertEquals(updatedConfigs.size, currentState.configs.size)
        assertEquals(newConfig1, currentState.configs[0])
        assertEquals(newConfig2, currentState.configs[1])

        val retrievedSettingsAgain = LineMarkerSettings.getInstance(project)
        assertSame(settings, retrievedSettingsAgain)
        assertEquals(currentState, retrievedSettingsAgain.state)
    }

    fun testUpdateWithEmptyListClearsSettings() {
        val initialConfig = LineMarkerConfig(name = "Initial Marker")
        settings.update(listOf(initialConfig))
        assertFalse(settings.state.configs.isEmpty())

        settings.update(emptyList())
        assertTrue(settings.state.configs.isEmpty())
    }

    fun testGetEnabledNonEmptyConfigs() {
        val enabledConfigWithRules = LineMarkerConfig(
            name = "Enabled With Rules",
            sqlTemplate = "SELECT 1",
            lineMarkerRules = mutableListOf(RangeRule("0", "1", "#000000")),
            enabled = true
        )
        val enabledConfigNoRules = LineMarkerConfig(
            name = "Enabled No Rules",
            sqlTemplate = "SELECT 1",
            lineMarkerRules = mutableListOf(),
            enabled = true
        )
        val enabledConfigBlankSql = LineMarkerConfig(
            name = "Enabled Blank SQL",
            sqlTemplate = " ",
            lineMarkerRules = mutableListOf(RangeRule("0", "1", "#000000")),
            enabled = true
        )
        val disabledConfigWithRules = LineMarkerConfig(
            name = "Disabled With Rules",
            sqlTemplate = "SELECT 1",
            lineMarkerRules = mutableListOf(RangeRule("0", "1", "#000000")),
            enabled = false
        )

        settings.update(
            listOf(
                enabledConfigWithRules,
                enabledConfigNoRules,
                enabledConfigBlankSql,
                disabledConfigWithRules
            )
        )

        val filteredConfigs = settings.getEnabledNonEmptyConfigs()
        assertEquals(1, filteredConfigs.size)
        assertTrue(filteredConfigs.contains(enabledConfigWithRules))
        assertFalse(filteredConfigs.contains(enabledConfigNoRules))
        assertFalse(filteredConfigs.contains(enabledConfigBlankSql))
        assertFalse(filteredConfigs.contains(disabledConfigWithRules))
    }

    fun testGetEnabledNonEmptyConfigsWhenAllAreEmptyOrDisabled() {
        val enabledConfigNoRules = LineMarkerConfig(
            name = "No Rules",
            sqlTemplate = "SELECT 1",
            lineMarkerRules = mutableListOf(),
            enabled = true
        )
        val enabledConfigBlankSql = LineMarkerConfig(
            name = "Blank SQL",
            sqlTemplate = "",
            lineMarkerRules = mutableListOf(RangeRule()),
            enabled = true
        )
        val disabledConfig = LineMarkerConfig(
            name = "Disabled",
            sqlTemplate = "SELECT 1",
            lineMarkerRules = mutableListOf(RangeRule()),
            enabled = false
        )

        settings.update(listOf(enabledConfigNoRules, enabledConfigBlankSql, disabledConfig))
        val filteredConfigs = settings.getEnabledNonEmptyConfigs()
        assertTrue(filteredConfigs.isEmpty())
    }

    fun testGetEnabledNonEmptyConfigsWhenSettingsAreEmpty() {
        settings.update(emptyList())
        val filteredConfigs = settings.getEnabledNonEmptyConfigs()
        assertTrue(filteredConfigs.isEmpty())
    }
}
