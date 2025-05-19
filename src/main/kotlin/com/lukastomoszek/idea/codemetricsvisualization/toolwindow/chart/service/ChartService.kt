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

package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.service

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.ChartSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ChartConfig
import com.lukastomoszek.idea.codemetricsvisualization.context.PsiContextResolver
import com.lukastomoszek.idea.codemetricsvisualization.context.PsiUtils
import com.lukastomoszek.idea.codemetricsvisualization.context.model.ContextInfo
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder
import com.lukastomoszek.idea.codemetricsvisualization.db.DuckDbService
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.model.ChartRequest
import com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.model.ChartResponse
import kotlinx.coroutines.*

@Service(Service.Level.PROJECT)
class ChartService(
    private val project: Project,
    private val cs: CoroutineScope
) {
    private var updateJob: Job? = null

    fun scheduleContextUpdate(onContextUpdated: suspend (ContextInfo) -> Unit) {
        updateJob?.cancel()
        updateJob = cs.launch {
            delay(300)
            if (project.isDisposed) return@launch

            val editor = readAction {
                FileEditorManager.getInstance(project).selectedTextEditor
            } ?: return@launch

            if (editor.isDisposed) return@launch
            val offset = readAction { editor.caretModel.offset }

            val psiFile = PsiUtils.getPsiFile(editor, project) ?: return@launch
            val psiElement = PsiUtils.findPsiElementAtOffset(psiFile, offset) ?: psiFile
            val contextInfo = psiElement.let { PsiContextResolver.getContextInfoFromPsi(it) }

            withContext(Dispatchers.EDT) {
                onContextUpdated(contextInfo)
            }
        }
    }

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
