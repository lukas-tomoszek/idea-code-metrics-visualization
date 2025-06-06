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

package com.lukastomoszek.idea.codemetricsvisualization.config.service

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DataSourceConfig
import com.lukastomoszek.idea.codemetricsvisualization.db.DuckDbService
import kotlinx.coroutines.*
import java.sql.SQLException

@Service(Service.Level.PROJECT)
class DataSourceService(private val project: Project, private val cs: CoroutineScope) {
    fun executeImport(config: DataSourceConfig, refreshCallback: () -> Unit = {}) {
        cs.launch {
            thisLogger().info("Executing import for '${config.name}'")
            val importSql = config.sql
            if (importSql.isBlank()) {
                showNotification("Import SQL for '${config.name}' is blank. Cannot execute.", NotificationType.WARNING)
                return@launch
            }

            showNotification("Executing import for '${config.name}'", NotificationType.INFORMATION)

            val result = try {
                withBackgroundProgress(project, "Executing import for '${config.name}'", true) {
                    DuckDbService.getInstance(project).executeWriteQuery(importSql)
                }
            } catch (e: Exception) {
                if (e is ControlFlowException || e is CancellationException) throw e
                Result.failure(e)
            }


            result.onSuccess {
                config.updateLastImportedTimestamp()
                showNotification("Import for '${config.name}' executed successfully.", NotificationType.INFORMATION)
                withContext(Dispatchers.EDT) {
                    refreshCallback()
                }
            }.onFailure { error ->
                if (error is ControlFlowException || error is CancellationException) throw error
                val msg = when (error) {
                    is SQLException -> "Database error during import of '${config.name}': ${error.message}"
                    else -> "Failed to import '${config.name}': ${error.message}"
                }
                thisLogger().error(msg, error)
                showNotification(msg, NotificationType.ERROR)
            }
        }
    }

    fun dropTable(tableName: String, callback: () -> Unit = {}) {
        cs.launch {
            if (tableName.isBlank()) {
                thisLogger().warn("Table name to drop is blank.")
                showNotification("Table name to drop cannot be blank.", NotificationType.WARNING)
                return@launch
            }

            val result = try {
                withBackgroundProgress(project, "Dropping table: $tableName", false) {
                    val sql = "DROP TABLE IF EXISTS \"$tableName\";"
                    DuckDbService.getInstance(project).executeWriteQuery(sql)
                }
            } catch (e: Exception) {
                if (e is ControlFlowException || e is CancellationException) throw e
                Result.failure(e)
            }

            result.onSuccess {
                showNotification("Table '$tableName' dropped successfully.", NotificationType.INFORMATION)
                withContext(Dispatchers.EDT) {
                    callback()
                }
            }.onFailure { error ->
                if (error is ControlFlowException || error is CancellationException) throw error
                val msg = "Error dropping table '$tableName': ${error.message}"
                thisLogger().error(msg, error)
                showNotification(msg, NotificationType.ERROR)
            }
        }
    }

    private suspend fun showNotification(message: String, type: NotificationType) {
        withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Code Metrics Visualization Notifications")
                .createNotification(message, type)
                .notify(project)
        }
    }

    companion object {
        fun getInstance(project: Project): DataSourceService =
            project.getService(DataSourceService::class.java)
    }
}
