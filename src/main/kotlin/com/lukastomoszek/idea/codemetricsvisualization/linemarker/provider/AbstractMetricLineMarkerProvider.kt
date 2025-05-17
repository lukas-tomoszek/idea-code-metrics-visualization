package com.lukastomoszek.idea.codemetricsvisualization.linemarker.provider

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.util.ui.ColorIcon
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.LineMarkerSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.context.PsiContextResolver
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder
import com.lukastomoszek.idea.codemetricsvisualization.db.DuckDbService
import com.lukastomoszek.idea.codemetricsvisualization.db.model.QueryResult
import com.lukastomoszek.idea.codemetricsvisualization.linemarker.rule.RuleEvaluator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

abstract class AbstractMetricLineMarkerProvider<T : PsiElement>(
    private val psiElementClass: Class<T>
) : LineMarkerProvider {

    abstract fun filterEnabledConfigs(configs: List<LineMarkerConfig>): List<LineMarkerConfig>
    open suspend fun preFilterElement(element: T, project: Project): Boolean = true
    abstract suspend fun getAnchorElement(element: T): PsiElement?

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(
        elements: List<PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        if (ApplicationManager.getApplication().isDispatchThread) {
            thisLogger().warn("collectSlowLineMarkers running on EDT!")
            return
        }

        val project = elements.firstOrNull()?.project ?: return
        val configs = filterEnabledConfigs(LineMarkerSettings.getInstance(project).getEnabledNonEmptyConfigs())
        if (configs.isEmpty()) return

        runBlockingCancellable {
            elements.asFlow()
                .filter { psiElementClass.isInstance(it) }
                .map { it as T }
                .filter { preFilterElement(it, project) }
                .map { processElementSafe(project, it, configs) }
                .toList()
                .flatten()
                .let(result::addAll)
        }
    }

    private suspend fun processElementSafe(
        project: Project,
        element: T,
        configs: List<LineMarkerConfig>
    ): List<LineMarkerInfo<*>> = try {
        processElement(project, element, configs)
    } catch (e: Exception) {
        if (e is ControlFlowException || e is CancellationException) throw e
        val desc = readAction { if (element.isValid) element.text.take(50) else "invalid element" }
        thisLogger().error("Error processing line marker: $desc", e)
        emptyList()
    }

    private suspend fun processElement(
        project: Project,
        originalElement: T,
        configs: List<LineMarkerConfig>
    ): List<LineMarkerInfo<*>> {
        val anchor = getAnchorElement(originalElement) ?: return emptyList()
        val range = readAction { anchor.takeIf { it.isValid }?.textRange } ?: return emptyList()
        val context = PsiContextResolver.getContextInfoFromPsi(originalElement)

        return configs.flatMap { config ->
            try {
                ContextAwareQueryBuilder.buildQuery(config.sqlTemplate, context).fold(
                    onSuccess = { sql ->
                        DuckDbService.getInstance(project).executeReadQuery(sql).fold(
                            onSuccess = { result ->
                                buildMarkerIfValid(anchor, range, config, extractMetricValue(result, sql))
                            },
                            onFailure = { e ->
                                logAndAddErrorMarker(e, sql, anchor, range, config)
                            }
                        )
                    },
                    onFailure = { error ->
                        thisLogger().info("Couldn't build SQL for line marker: ${config.name}", error)
                        emptyList()
                    }
                )
            } catch (e: Exception) {
                if (e is ControlFlowException || e is CancellationException) throw e
                val desc =
                    readAction { if (originalElement.isValid) originalElement.text.take(50) else "invalid element" }
                logAndAddErrorMarker(
                    e,
                    config.sqlTemplate,
                    anchor,
                    range,
                    config,
                    "Error processing config '${config.name}' for '$desc'"
                )
            }
        }
    }

    private fun buildMarkerIfValid(
        anchor: PsiElement,
        range: TextRange,
        config: LineMarkerConfig,
        value: Float?
    ): List<LineMarkerInfo<*>> {
        if (value == null) return emptyList()
        val color = RuleEvaluator.evaluate(value, config.lineMarkerRules) ?: return emptyList()
        val tooltip =
            { _: PsiElement? -> "${config.name}: ${if (value % 1 == 0f) "%.0f".format(value) else "%.2f".format(value)}" }

        return listOf(
            LineMarkerInfo(anchor, range, ColorIcon(10, color), tooltip, null, GutterIconRenderer.Alignment.LEFT) {
                "Production Code Metric Visualization (${config.name})"
            }
        )
    }

    private fun extractMetricValue(result: QueryResult, sql: String): Float? {
        return result.columnNames.firstOrNull()?.let { col ->
            result.rows.firstOrNull()?.get(col)?.toString()?.toFloatOrNull()
        }.also {
            if (it == null) {
                val msg = if (result.rows.isEmpty()) "no rows" else "no columns"
                thisLogger().trace("Query returned $msg. SQL: $sql")
            }
        }
    }

    private fun logAndAddErrorMarker(
        error: Throwable,
        sql: String,
        anchor: PsiElement,
        range: TextRange,
        config: LineMarkerConfig,
        prefix: String = "Error"
    ): List<LineMarkerInfo<*>> {
        if (error is ControlFlowException || error is CancellationException) throw error
        val msg = "$prefix in '${config.name}': ${error.message?.take(100)} SQL: ${sql.take(100)}"
        thisLogger().warn(msg, error)
        return listOf(createErrorMarker(anchor, range, config, msg))
    }

    private fun createErrorMarker(
        anchor: PsiElement,
        range: TextRange,
        config: LineMarkerConfig,
        errorMessage: String
    ): LineMarkerInfo<*> {
        val icon = IconLoader.getIcon("/icons/breakpointObsolete.svg", this.javaClass)
        val tooltip = { _: PsiElement? -> "Error in '${config.name}': $errorMessage" }
        return LineMarkerInfo(anchor, range, icon, tooltip, null, GutterIconRenderer.Alignment.LEFT) {
            "Line Marker Error (${config.name})"
        }
    }
}
