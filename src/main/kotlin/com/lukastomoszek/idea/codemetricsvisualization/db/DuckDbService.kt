package com.lukastomoszek.idea.codemetricsvisualization.db

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.lukastomoszek.idea.codemetricsvisualization.db.model.QueryResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSetMetaData
import java.sql.SQLException
import java.util.*

@Service(Service.Level.PROJECT)
class DuckDbService(private val project: Project) {

    private val dbFile: File
        get() = File(project.basePath, ".idea/codeMetricsVisualizations/data.db")

    private var driverLoaded = false
    private val writeMutex = Mutex()

    init {
        loadDriver()
    }

    private fun loadDriver() {
        try {
            Class.forName("org.duckdb.DuckDBDriver")
            driverLoaded = true
        } catch (e: ClassNotFoundException) {
            driverLoaded = false
            thisLogger().error("Failed to load DuckDB JDBC driver.", e)
        }
    }

    private fun ensureDbDirectoryExists(): Boolean {
        if (!driverLoaded) return false
        val dbDir = dbFile.parentFile
        if (!dbDir.exists()) {
            if (!dbDir.mkdirs()) {
                thisLogger().error("Failed to create database directory: ${dbDir.absolutePath}")
                return false
            }
        } else if (!dbDir.isDirectory) {
            thisLogger().error("Database path parent exists but is not a directory: ${dbDir.absolutePath}")
            return false
        }
        return true
    }

    private fun getConnection(readOnly: Boolean = false): Connection {
        if (!driverLoaded) {
            throw SQLException("DuckDB driver not loaded.")
        }
        if (!ensureDbDirectoryExists()) {
            throw SQLException("Failed to ensure database directory exists.")
        }
        val props = Properties()
        if (readOnly) {
            if (!dbFile.exists()) {
                throw SQLException("Database file does not exist at ${dbFile.absolutePath}. Import data first.")
            }
            props.setProperty("duckdb.read_only", "true")
        }
        return DriverManager.getConnection("jdbc:duckdb:${dbFile.absolutePath}", props)
    }

    suspend fun executeWriteQuery(sql: String): Result<Unit> {
        if (sql.isBlank()) {
            thisLogger().warn("Write SQL command is blank, skipping execution.")
            return Result.failure(IllegalArgumentException("Write SQL command cannot be blank."))
        }

        return writeMutex.withLock {
            runCatching {
                getConnection(readOnly = false).use { conn ->
                    conn.createStatement().use { statement ->
                        thisLogger().info("Executing write SQL: $sql")
                        statement.execute(sql)
                    }
                }
                thisLogger().info("Successfully executed write SQL.")
                QueryCacheService.getInstance(project).clear()
            }.onFailure { e ->
                val errorMessage = when (e) {
                    is SQLException -> "SQL Error executing write query '$sql': ${e.message}"
                    else -> "Unexpected error during write SQL for query '$sql': ${e.message}"
                }
                thisLogger().error(errorMessage, e)
            }
        }
    }

    fun executeReadQuery(sql: String): Result<QueryResult> {
        if (sql.isBlank()) {
            thisLogger().warn("Read SQL command is blank, skipping execution.")
            return Result.failure(IllegalArgumentException("Read SQL command cannot be blank."))
        }

        QueryCacheService.getInstance(project).get(sql)?.let {
            return it
        }

        if (writeMutex.isLocked) {
            val errorMessage = "Database is currently busy with a write operation."
            thisLogger().debug("$errorMessage SQL: $sql")
            return Result.failure(SQLException(errorMessage))
        }

        return runCatching {
            getConnection(readOnly = true).use { conn ->
                conn.createStatement().use { stmt ->
                    thisLogger().debug("Executing read SQL: $sql")
                    stmt.executeQuery(sql).use { rs ->
                        val metaData: ResultSetMetaData = rs.metaData
                        val columnCount = metaData.columnCount
                        val columnNames = List(columnCount) { i -> metaData.getColumnLabel(i + 1) }
                        val columnTypes = List(columnCount) { i -> metaData.getColumnTypeName(i + 1) }

                        val results = mutableListOf<Map<String, Any?>>()
                        while (rs.next()) {
                            val rowMap = mutableMapOf<String, Any?>()
                            for (i in 1..columnCount) {
                                rowMap[columnNames[i - 1]] = rs.getObject(i)
                            }
                            results.add(Collections.unmodifiableMap(rowMap))
                        }
                        QueryResult(columnNames, columnTypes, results)
                    }
                }
            }
        }.onSuccess { queryResult ->
            QueryCacheService.getInstance(project).put(sql, Result.success(queryResult))
        }.onFailure { e ->
            val errorMessage = when (e) {
                is SQLException -> "Database Read Error for query '$sql': ${e.message}"
                else -> "Unexpected Read Error for query '$sql': ${e.message}"
            }
            thisLogger().warn(errorMessage, e)
        }
    }

    companion object {
        fun getInstance(project: Project): DuckDbService =
            project.getService(DuckDbService::class.java)
    }
}
