package com.lukastomoszek.idea.codemetricsvisualization.config.state

import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.XCollection
import com.lukastomoszek.idea.codemetricsvisualization.config.state.util.LocalDateTimeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal object DefaultDataSource {
    const val NAME = "New Data Source"
    const val TABLE_NAME = "new_table"
    const val FILE_PATH = ""
    val IMPORT_MODE = ImportMode.REPLACE
    val SQL: String = """
CREATE OR REPLACE TABLE $TABLE_NAME AS
SELECT *
FROM read_csv('/path/to/your/data.csv', header=true);
    """.trimIndent()
}

enum class ImportMode {
    REPLACE, APPEND
}

data class DataSourceSettingsState(
    @XCollection
    val dataSources: List<DataSourceConfig> = listOf()
)

data class DataSourceConfig(
    var name: String = DefaultDataSource.NAME,
    var tableName: String = DefaultDataSource.TABLE_NAME,
    var filePath: String = DefaultDataSource.FILE_PATH,
    var importMode: ImportMode = DefaultDataSource.IMPORT_MODE,
    @OptionTag(converter = LocalDateTimeConverter::class)
    var lastImportedAt: LocalDateTime? = null,
    var sql: String = DefaultDataSource.SQL
) {
    fun getFormattedLastImportedAt(): String =
        lastImportedAt?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) ?: "Never"

    fun updateLastImportedTimestamp() {
        lastImportedAt = LocalDateTime.now()
    }
}
