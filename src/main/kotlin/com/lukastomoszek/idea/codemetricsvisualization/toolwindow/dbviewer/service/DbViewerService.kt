package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.dbviewer.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.lukastomoszek.idea.codemetricsvisualization.db.DuckDbService
import com.lukastomoszek.idea.codemetricsvisualization.db.model.QueryResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class DbViewerService(
    private val project: Project,
    private val cs: CoroutineScope
) {

    fun getTableNames(callback: (Result<List<String>>) -> Unit) {
        cs.launch {
            val result = withBackgroundProgress(project, "Fetching Table Names", false) {
                DuckDbService.getInstance(project).executeReadQuery("SHOW TABLES;")
                    .mapCatching { result ->
                        result.rows.mapNotNull { it["name"] as? String }.sorted()
                    }
            }
            callback(result)
        }
    }

    fun getTableColumns(tableName: String, callback: (Result<List<String>>) -> Unit) {
        cs.launch {
            val result = withBackgroundProgress(project, "Fetching Columns for '$tableName'", false) {
                DuckDbService.getInstance(project)
                    .executeReadQuery("SELECT * FROM \"$tableName\" LIMIT 0")
                    .mapCatching { it.columnNames }
            }
            callback(result)
        }
    }

    fun queryTableData(sqlQuery: String, callback: (Result<QueryResult>) -> Unit) {
        cs.launch {
            val result = withBackgroundProgress(project, "Loading Data for DB Viewer", false) {
                DuckDbService.getInstance(project).executeReadQuery(sqlQuery)
            }
            callback(result)
        }
    }

    companion object {
        fun getInstance(project: Project): DbViewerService =
            project.getService(DbViewerService::class.java)
    }
}
