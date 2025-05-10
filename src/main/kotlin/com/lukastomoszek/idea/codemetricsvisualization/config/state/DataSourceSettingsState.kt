package com.lukastomoszek.idea.codemetricsvisualization.config.state

import com.intellij.util.xmlb.annotations.XCollection
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

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
    var dataSources: MutableList<DataSourceConfig> = mutableListOf()
)

data class DataSourceConfig(
    var name: String = DefaultDataSource.NAME,
    var tableName: String = DefaultDataSource.TABLE_NAME,
    var filePath: String = DefaultDataSource.FILE_PATH,
    var importMode: ImportMode = DefaultDataSource.IMPORT_MODE,
    var lastImportedAt: String? = null,
    var sql: String = DefaultDataSource.SQL
) {
    fun getFormattedLastImportedAt(): String {
        return lastImportedAt?.let {
            try {
                LocalDateTime.parse(it).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            } catch (_: DateTimeParseException) {
                "Invalid Date"
            }
        } ?: "Never"
    }

    fun updateLastImportedTimestamp() {
        lastImportedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}
