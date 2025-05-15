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
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DataSourceConfig
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

            runCatching {
                withContext(Dispatchers.IO) {
                    CopyPasteManager.getInstance().setContents(StringSelection(prompt))
                }
            }.getOrElse {
                showNotification("Failed to copy prompt to clipboard.", NotificationType.ERROR)
                return@launch
            }

            showNotification("AI prompt for '${config.name}' copied to clipboard.", NotificationType.INFORMATION)
        }
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
