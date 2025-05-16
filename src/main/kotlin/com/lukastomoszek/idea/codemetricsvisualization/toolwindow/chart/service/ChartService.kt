package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.service

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.ChartSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ChartConfig
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder
import com.lukastomoszek.idea.codemetricsvisualization.db.DuckDbService
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.model.ChartRequest
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.model.ChartResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class ChartService(
    private val project: Project,
    private val cs: CoroutineScope
) {

    fun getAvailableChartConfigs(): List<ChartConfig> {
        return ChartSettings.getInstance(project).state.configs.toList()
    }

    fun fetchChartData(request: ChartRequest, callback: (ChartResponse) -> Unit) {
        val querySqlTemplate = request.config.sqlTemplate
        val chartName = request.config.name

        if (querySqlTemplate.isBlank()) {
            val response =
                ChartResponse(errorMessage = "Chart query for '$chartName' is empty.", originalRequest = request)
            callback(response)
            return
        }

        cs.launch {
            val result =
                withBackgroundProgress(project, "Loading Chart '$chartName'", cancellable = true) {
                    val builtSqlQueryResult = ContextAwareQueryBuilder.buildQuery(
                        querySqlTemplate,
                        request.contextInfo,
                        true
                    )

                    builtSqlQueryResult.fold(
                        onSuccess = { finalSql ->
                            DuckDbService.getInstance(project).executeReadQuery(finalSql).fold(
                                onSuccess = { queryResult ->
                                    if (queryResult.columnNames.size < 2 && queryResult.rows.isNotEmpty()) {
                                        ChartResponse(
                                            errorMessage = "Query '$chartName' must return at least 2 columns (Label, Value), but returned ${queryResult.columnNames.size}.",
                                            originalRequest = request
                                        )
                                    } else {
                                        ChartResponse(queryResult = queryResult, originalRequest = request)
                                    }
                                },
                                onFailure = { dbError ->
                                    ChartResponse(
                                        errorMessage = dbError.message ?: "Unknown database error.",
                                        originalRequest = request
                                    )
                                }
                            )
                        },
                        onFailure = { buildError ->
                            ChartResponse(
                                errorMessage = "Failed to build SQL: ${buildError.message}",
                                originalRequest = request
                            )
                        }
                    )
                }
            withContext(Dispatchers.EDT) {
                callback(result)
            }
        }
    }

    companion object {
        fun getInstance(project: Project): ChartService = project.getService(ChartService::class.java)
    }
}
