package com.lukastomoszek.idea.codemetricsvisualization.config.persistence

import com.intellij.testFramework.LightPlatformTestCase
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DataSourceConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DataSourceSettingsState
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ImportMode

class DataSourceSettingsPersistenceTest : LightPlatformTestCase() {

    private lateinit var settings: DataSourceSettings

    override fun setUp() {
        super.setUp()
        settings = DataSourceSettings.getInstance(project)
        settings.update(emptyList())
    }

    fun testDefaultSettingsState() {
        assertNotNull(settings)
        assertEquals(DataSourceSettingsState(), settings.state)
        assertTrue(settings.state.configs.isEmpty())
    }

    fun testUpdateAndRetrieveSettings() {
        val initialConfigs = settings.state.configs.toList()
        assertTrue(initialConfigs.isEmpty())

        val newDataSourceConfig1 = DataSourceConfig(
            name = "Source 1",
            tableName = "table1",
            filePath = "/path/1",
            importMode = ImportMode.REPLACE
        )
        val newDataSourceConfig2 = DataSourceConfig(
            name = "Source 2",
            tableName = "table2",
            filePath = "/path/2",
            importMode = ImportMode.APPEND
        )
        val updatedConfigs = listOf(newDataSourceConfig1, newDataSourceConfig2)

        settings.update(updatedConfigs)

        val currentState = settings.state
        assertEquals(updatedConfigs.size, currentState.configs.size)
        assertEquals(newDataSourceConfig1, currentState.configs[0])
        assertEquals(newDataSourceConfig2, currentState.configs[1])

        val retrievedSettingsAgain = DataSourceSettings.getInstance(project)
        assertSame(settings, retrievedSettingsAgain)
        assertEquals(currentState, retrievedSettingsAgain.state)
    }

    fun testUpdateWithEmptyListClearsSettings() {
        val initialConfig = DataSourceConfig(name = "Initial Source")
        settings.update(listOf(initialConfig))
        assertFalse(settings.state.configs.isEmpty())

        settings.update(emptyList())
        assertTrue(settings.state.configs.isEmpty())
    }
}
