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

package com.lukastomoszek.idea.codemetricsvisualization.db

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.lukastomoszek.idea.codemetricsvisualization.db.model.QueryResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

        return withContext(Dispatchers.IO) {
            runCatching {
                getConnection(readOnly = false).use { conn ->
                    conn.createStatement().use { statement ->
                        thisLogger().info("Executing write SQL: $sql")
                        statement.execute(sql)
                    }
                }
                thisLogger().info("Successfully executed write SQL.")
                QueryCacheService.getInstance(project).clear()
            }.onFailure { error ->
                if (error is ControlFlowException || error is CancellationException) throw error
                val errorMessage = when (error) {
                    is SQLException -> "SQL Error executing write query '$sql': ${error.message}"
                    else -> "Unexpected error during write SQL for query '$sql': ${error.message}"
                }
                thisLogger().warn(errorMessage, error)
            }
        }
    }

    suspend fun executeReadQuery(sql: String): Result<QueryResult> {
        if (sql.isBlank()) {
            thisLogger().warn("Read SQL command is blank, skipping execution.")
            return Result.failure(IllegalArgumentException("Read SQL command cannot be blank."))
        }

        QueryCacheService.getInstance(project).get(sql)?.let {
            return it
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                getConnection(readOnly = true).use { conn ->
                    conn.createStatement().use { stmt ->
                        stmt.queryTimeout = READ_QUERY_TIMEOUT_SECONDS
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
            }.onFailure { error ->
                if (error is ControlFlowException || error is CancellationException) throw error
                val errorMessage = when (error) {
                    is SQLException -> "Database Read Error for query '$sql': ${error.message}"
                    else -> "Unexpected Read Error for query '$sql': ${error.message}"
                }
                thisLogger().warn(errorMessage, error)
            }
        }
    }

    companion object {
        const val READ_QUERY_TIMEOUT_SECONDS = 10

        fun getInstance(project: Project): DuckDbService =
            project.getService(DuckDbService::class.java)
    }
}
