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
import com.intellij.util.Function
import com.intellij.util.ui.ColorIcon
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.LineMarkerSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.context.PsiContextResolver
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder
import com.lukastomoszek.idea.codemetricsvisualization.db.DuckDbService
import com.lukastomoszek.idea.codemetricsvisualization.db.model.QueryResult
import com.lukastomoszek.idea.codemetricsvisualization.linemarker.rule.RuleEvaluator
import com.lukastomoszek.idea.codemetricsvisualization.util.FormattingUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import javax.swing.Icon

abstract class AbstractMetricLineMarkerProvider<T : PsiElement>(
    private val psiElementClass: Class<T>
) : LineMarkerProvider {

    abstract fun filterEnabledConfigs(allEnabledConfigs: List<LineMarkerConfig>): List<LineMarkerConfig>
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

        if (elements.isEmpty()) return

        val project = elements.first().project
        val lineMarkerSettings = LineMarkerSettings.getInstance(project)
        val allEnabledConfigs = lineMarkerSettings.getEnabledNonEmptyConfigs()
        val relevantConfigs = filterEnabledConfigs(allEnabledConfigs)

        if (relevantConfigs.isEmpty()) {
            return
        }

        runBlockingCancellable {
            val markers = elements.asFlow()
                .filter { psiElementClass.isInstance(it) }
                .map {
                    @Suppress("UNCHECKED_CAST")
                    it as T
                }
                .filter { preFilterElement(it, project) }
                .map { typedElement ->
                    try {
                        processElementSuspending(project, typedElement, relevantConfigs)
                    } catch (e: Exception) {
                        if (e is ControlFlowException || e is CancellationException) throw e
                        val elementDescription = readAction {
                            if (typedElement.isValid) typedElement.text.take(50) else "invalid element"
                        }
                        thisLogger().error("Error processing element for line marker: $elementDescription", e)
                        emptyList()
                    }
                }.toList()
            result.addAll(markers.flatten())
        }
    }

    private suspend fun processElementSuspending(
        project: Project,
        originalElement: T,
        configs: List<LineMarkerConfig>
    ): List<LineMarkerInfo<*>> {
        val anchorElement = getAnchorElement(originalElement) ?: return emptyList()
        val contextInfo = PsiContextResolver.getContextInfoFromPsi(originalElement)
        val anchorTextRange = readAction {
            if (anchorElement.isValid) anchorElement.textRange else null
        } ?: return emptyList()

        val elementMarkers = mutableListOf<LineMarkerInfo<*>>()

        configs.forEach { config ->
            try {
                val builtQueryResult = ContextAwareQueryBuilder.buildQuery(
                    config.sqlTemplate,
                    contextInfo
                )

                builtQueryResult.fold(
                    onSuccess = { finalSql ->
                        val queryResultOutcome = DuckDbService.getInstance(project).executeReadQuery(finalSql)
                        queryResultOutcome.fold(
                            onSuccess = { queryResult ->
                                val metricValue = extractMetricValue(queryResult, finalSql)
                                val displayColor = RuleEvaluator.evaluate(metricValue, config.lineMarkerRules)

                                if (metricValue != null && displayColor != null) {
                                    val icon: Icon = ColorIcon(10, displayColor)
                                    val tooltipText = "${config.name}: ${FormattingUtils.formatNumber(metricValue)}"
                                    val tooltipProvider = Function { _: PsiElement? -> tooltipText }

                                    val info = LineMarkerInfo(
                                        anchorElement,
                                        anchorTextRange,
                                        icon,
                                        tooltipProvider,
                                        null,
                                        GutterIconRenderer.Alignment.LEFT,
                                    ) { "Production Code Metric Visualization (${config.name})" }
                                    elementMarkers.add(info)
                                }
                            },
                            onFailure = { error ->
                                if (error is ControlFlowException || error is CancellationException) throw error
                                val errorMessage = "DB Error: ${error.message?.take(100)} SQL: ${finalSql.take(100)}"
                                thisLogger().warn("$errorMessage...", error)
                                addErrorMarker(anchorElement, anchorTextRange, config, errorMessage, elementMarkers)
                            }
                        )
                    },
                    onFailure = { error ->
                        if (error is ControlFlowException || error is CancellationException) throw error
                        val errorMessage = "SQL build failed for '${config.name}': ${error.message?.take(100)}"
                        thisLogger().trace("$errorMessage...")
                        addErrorMarker(anchorElement, anchorTextRange, config, errorMessage, elementMarkers)
                    }
                )
            } catch (e: Exception) {
                if (e is ControlFlowException || e is CancellationException) throw e
                val originalElementTextForError = readAction { originalElement.text } ?: "invalid element"
                val errorMessage =
                    "Error for element '$originalElementTextForError', config '${config.name}': ${e.message?.take(100)}"
                thisLogger().error("$errorMessage...", e)
                addErrorMarker(anchorElement, anchorTextRange, config, errorMessage, elementMarkers)
            }
        }
        return elementMarkers
    }

    private fun extractMetricValue(queryResult: QueryResult, finalSql: String): Float? {
        return if (queryResult.rows.isNotEmpty() && queryResult.columnNames.isNotEmpty()) {
            queryResult.rows.first()[queryResult.columnNames.first()].toString().toFloatOrNull()
        } else {
            if (queryResult.rows.isEmpty()) {
                thisLogger().trace("Query returned no rows for single metric value. SQL: $finalSql")
            } else {
                thisLogger().warn("Query returned no columns for single metric value. SQL: $finalSql")
            }
            null
        }
    }

    protected suspend fun addErrorMarker(
        anchorElement: PsiElement?,
        anchorTextRange: TextRange,
        config: LineMarkerConfig,
        errorMessage: String,
        elementMarkers: MutableList<in LineMarkerInfo<*>>
    ) {
        if (anchorElement == null || !readAction { anchorElement.isValid }) return

        val errorIcon: Icon = IconLoader.getIcon("/icons/breakpointObsolete.svg", this.javaClass)
        val tooltipProvider = { _: PsiElement? ->
            "Error in '${config.name}': $errorMessage"
        }
        val errorInfo = LineMarkerInfo(
            anchorElement,
            anchorTextRange,
            errorIcon,
            tooltipProvider,
            null,
            GutterIconRenderer.Alignment.LEFT,
        ) { "Line Marker Error (${config.name})" }
        elementMarkers.add(errorInfo)
    }
}
