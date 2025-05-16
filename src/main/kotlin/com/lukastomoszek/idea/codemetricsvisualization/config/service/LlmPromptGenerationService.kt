package com.lukastomoszek.idea.codemetricsvisualization.config.service

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ChartConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DataSourceConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.db.DuckDbService
import com.lukastomoszek.idea.codemetricsvisualization.db.model.QueryResult
import kotlinx.coroutines.*
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

@Service(Service.Level.PROJECT)
class LlmPromptGenerationService(
    private val project: Project,
    private val cs: CoroutineScope
) {

    fun generateDataSourceImportPrompt(config: DataSourceConfig) {
        cs.launch {
            val prompt = runCatching {
                val fileSample = withContext(Dispatchers.IO) { resolveFileSample(config.filePath) }
                val template = loadTemplate("import-data-source.txt")
                createDataSourcePrompt(config, fileSample, template)
            }.getOrElse { e ->
                if (e is ControlFlowException || e is CancellationException) throw e
                val msg = "Failed to generate prompt for '${config.tableName}': ${e.message}"
                thisLogger().warn(msg, e)
                showNotification(msg, NotificationType.ERROR)
                return@launch
            }
            copyToClipboardAndNotify(prompt, "AI prompt for '${config.name}' copied to clipboard.")
        }
    }

    fun generateLineMarkerSqlPrompt(config: LineMarkerConfig) {
        cs.launch {
            val prompt = runCatching {
                val tableSamples = fetchTableSamples(config.llmRelevantTableNames)
                val template = loadTemplate("line-marker-sql.txt")
                createLineMarkerPrompt(config, tableSamples, template)
            }.getOrElse { e ->
                if (e is ControlFlowException || e is CancellationException) throw e
                val msg = "Failed to generate Line Marker prompt for '${config.name}': ${e.message}"
                thisLogger().warn(msg, e)
                showNotification(msg, NotificationType.ERROR)
                return@launch
            }
            copyToClipboardAndNotify(prompt, "AI prompt for Line Marker '${config.name}' copied to clipboard.")
        }
    }

    fun generateChartSqlPrompt(config: ChartConfig) {
        cs.launch {
            val prompt = runCatching {
                val tableSamples = fetchTableSamples(config.llmRelevantTableNames)
                val template = loadTemplate("chart-sql.txt")
                createChartPrompt(config, tableSamples, template)
            }.getOrElse { e ->
                if (e is ControlFlowException || e is CancellationException) throw e
                val msg = "Failed to generate Chart prompt for '${config.name}': ${e.message}"
                thisLogger().warn(msg, e)
                showNotification(msg, NotificationType.ERROR)
                return@launch
            }
            copyToClipboardAndNotify(prompt, "AI prompt for Chart '${config.name}' copied to clipboard.")
        }
    }

    private suspend fun copyToClipboardAndNotify(prompt: String, successMessage: String) {
        runCatching {
            withContext(Dispatchers.IO) {
                CopyPasteManager.getInstance().setContents(StringSelection(prompt))
            }
        }.getOrElse {
            showNotification("Failed to copy prompt to clipboard.", NotificationType.ERROR)
            return
        }
        showNotification(successMessage, NotificationType.INFORMATION)
    }

    private suspend fun fetchTableSamples(tableNames: List<String>): String {
        if (tableNames.isEmpty()) return "No relevant tables specified."
        val duckDbService = DuckDbService.getInstance(project)
        return tableNames.joinToString("\n\n") { tableName ->
            val sampleQuery = "SELECT * FROM \"$tableName\" USING SAMPLE 10 ROWS;"
            val result = duckDbService.executeReadQuery(sampleQuery)
            formatTableSample(tableName, result)
        }
    }

    private fun formatTableSample(tableName: String, result: Result<QueryResult>): String {
        return result.fold(
            onSuccess = { queryResult ->
                if (queryResult.rows.isEmpty()) {
                    "Table '$tableName' (Sample - 0 rows):\nNo rows in sample or table is empty.\nColumns: ${
                        queryResult.columnNames.joinToString(", ")
                    }"
                } else {
                    val header = queryResult.columnNames.joinToString(" | ")
                    val rows = queryResult.rows.take(10).joinToString("\n") { row ->
                        queryResult.columnNames.joinToString(" | ") { colName ->
                            (row[colName]?.toString() ?: "NULL").take(30)
                        }
                    }
                    "Table '$tableName' (Sample - ${queryResult.rows.size} rows):\n$header\n$rows"
                }
            },
            onFailure = { error ->
                "Could not fetch sample for table '$tableName': ${error.message}"
            }
        )
    }

    private fun resolveFileSample(filePath: String?): String {
        if (filePath.isNullOrBlank()) throw IOException("File path is empty.")
        val file = File(filePath)
        if (!file.exists() || !file.isFile) throw IOException("File not found: $filePath")
        return file.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
            lines.take(10).joinToString("\n")
        }
    }

    private fun createDataSourcePrompt(config: DataSourceConfig, fileSample: String, template: String): String {
        return template
            .replace("{{filePath}}", config.filePath)
            .replace("{{tableName}}", config.tableName)
            .replace("{{importMode}}", config.importMode.name.lowercase())
            .replace("{{fileSample}}", fileSample)
            .replace("{{additionalInfo}}", config.llmAdditionalInfo)
    }

    private fun createLineMarkerPrompt(config: LineMarkerConfig, tableSamples: String, template: String): String {
        return template
            .replace("{{llmDescription}}", config.llmDescription)
            .replace("{{tableSamples}}", tableSamples)
    }

    private fun createChartPrompt(config: ChartConfig, tableSamples: String, template: String): String {
        return template
            .replace("{{llmDescription}}", config.llmDescription)
            .replace("{{tableSamples}}", tableSamples)
    }

    private suspend fun loadTemplate(fileName: String): String {
        val path = "$PROMPT_TEMPLATES_BASE_PATH$fileName"
        val stream = LlmPromptGenerationService::class.java.getResourceAsStream(path)
                     ?: throw IOException("Template '$fileName' not found at '$path'")
        return withContext(Dispatchers.IO) {
            stream.use { it.readAllBytes().toString(StandardCharsets.UTF_8) }
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
        private const val PROMPT_TEMPLATES_BASE_PATH = "/prompt-templates/"

        fun getInstance(project: Project): LlmPromptGenerationService =
            project.getService(LlmPromptGenerationService::class.java)
    }
}
