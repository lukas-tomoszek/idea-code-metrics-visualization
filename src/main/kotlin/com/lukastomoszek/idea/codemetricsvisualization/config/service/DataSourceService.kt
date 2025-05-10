package com.lukastomoszek.idea.codemetricsvisualization.config.service

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DataSourceConfig
import com.lukastomoszek.idea.codemetricsvisualization.db.DuckDbService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.SQLException

@Service(Service.Level.PROJECT)
class DataSourceService(
    private val project: Project,
    private val cs: CoroutineScope
) {
    fun executeImport(config: DataSourceConfig, refreshCallback: () -> Unit = {}) {
        val importSql = config.sql
        if (importSql.isBlank()) {
            cs.launch {
                showNotification("Import SQL for '${config.name}' is blank. Cannot execute.", NotificationType.WARNING)
            }
            return
        }


        cs.launch {
            showNotification("Executing import for '${config.name}'", NotificationType.INFORMATION)

            val result = withBackgroundProgress(project, "Executing import for '${config.name}'", false) {
                DuckDbService.getInstance(project).executeWriteQuery(importSql)
            }

            result.onSuccess {
                config.updateLastImportedTimestamp()
                showNotification("Import for '${config.name}' executed successfully.", NotificationType.INFORMATION)
                refreshCallback()
            }.onFailure { error ->
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
        if (tableName.isBlank()) {
            thisLogger().warn("Table name to drop is blank.")
            cs.launch { showNotification("Table name to drop cannot be blank.", NotificationType.WARNING) }
            return
        }

        cs.launch {
            val result = withBackgroundProgress(project, "Dropping table: $tableName", false) {
                val sql = "DROP TABLE IF EXISTS \"$tableName\";"
                DuckDbService.getInstance(project).executeWriteQuery(sql)
            }

            result.onSuccess {
                showNotification("Table '$tableName' dropped successfully.", NotificationType.INFORMATION)
                callback()
            }.onFailure { error ->
                val msg = "Error dropping table '$tableName': ${error.message}"
                thisLogger().error(msg, error)
                showNotification(msg, NotificationType.ERROR)
            }
        }
    }

    private suspend fun showNotification(message: String, type: NotificationType) {
        withContext(Dispatchers.Main) {
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
