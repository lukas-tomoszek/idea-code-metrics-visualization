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

package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.dbviewer.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.lukastomoszek.idea.codemetricsvisualization.db.DuckDbService
import com.lukastomoszek.idea.codemetricsvisualization.db.model.QueryResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class DbViewerService(
    private val project: Project,
    private val cs: CoroutineScope
) {

    fun getTableNames(callback: (Result<List<String>>) -> Unit) {
        cs.launch {
            val result = try {
                withBackgroundProgress(project, "Fetching Table Names", false) {
                    DuckDbService.getInstance(project).executeReadQuery("SHOW TABLES;")
                        .mapCatching { queryResult ->
                            queryResult.rows.mapNotNull { it["name"] as? String }.sorted()
                        }
                }
            } catch (e: Exception) {
                if (e is ControlFlowException || e is CancellationException) throw e
                Result.failure(e)
            }
            callback(result)
        }
    }

    fun getTableColumns(tableName: String, callback: (Result<List<String>>) -> Unit) {
        cs.launch {
            val result = try {
                withBackgroundProgress(project, "Fetching Columns for '$tableName'", false) {
                    DuckDbService.getInstance(project)
                        .executeReadQuery("SELECT * FROM \"$tableName\" LIMIT 0")
                        .mapCatching { it.columnNames }
                }
            } catch (e: Exception) {
                if (e is ControlFlowException || e is CancellationException) throw e
                Result.failure(e)
            }
            callback(result)
        }
    }

    fun queryTableData(sqlQuery: String, callback: (Result<QueryResult>) -> Unit) {
        cs.launch {
            val result = try {
                withBackgroundProgress(project, "Loading Data for DB Viewer", false) {
                    DuckDbService.getInstance(project).executeReadQuery(sqlQuery)
                }
            } catch (e: Exception) {
                if (e is ControlFlowException || e is CancellationException) throw e
                Result.failure(e)
            }
            callback(result)
        }
    }

    fun getTableRowCount(tableName: String, callback: (Result<Long?>) -> Unit) {
        cs.launch {
            val result = try {
                withBackgroundProgress(project, "Fetching Row Count for '$tableName'", false) {
                    val countQuery = "SELECT COUNT(*) FROM \"$tableName\";"
                    DuckDbService.getInstance(project).executeReadQuery(countQuery)
                        .mapCatching { queryResult ->
                            queryResult.rows.firstOrNull()?.values?.firstOrNull() as? Long
                        }
                }
            } catch (e: Exception) {
                if (e is ControlFlowException || e is CancellationException) throw e
                Result.failure(e)
            }
            callback(result)
        }
    }

    companion object {
        fun getInstance(project: Project): DbViewerService =
            project.getService(DbViewerService::class.java)
    }
}
