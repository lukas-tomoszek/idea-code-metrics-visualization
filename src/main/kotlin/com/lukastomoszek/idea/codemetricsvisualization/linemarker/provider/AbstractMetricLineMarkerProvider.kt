package com.lukastomoszek.idea.codemetricsvisualization.linemarker.provider

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import com.intellij.util.Function
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.ColorIcon
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.LineMarkerSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder
import com.lukastomoszek.idea.codemetricsvisualization.db.DuckDbService
import com.lukastomoszek.idea.codemetricsvisualization.db.model.QueryResult
import com.lukastomoszek.idea.codemetricsvisualization.linemarker.rule.RuleEvaluator
import com.lukastomoszek.idea.codemetricsvisualization.util.FormattingUtils
import java.util.concurrent.Callable
import java.util.concurrent.Future
import javax.swing.Icon

abstract class AbstractMetricLineMarkerProvider<T : PsiElement>(
    private val psiElementClass: Class<T>
) : LineMarkerProvider, DumbAware {

    abstract fun filterEnabledConfigs(allEnabledConfigs: List<LineMarkerConfig>): List<LineMarkerConfig>
    open fun preFilterElement(element: T, project: Project): Boolean = true
    abstract fun getAnchorElement(element: T): PsiElement?
    abstract fun getLineMarkerGroupName(config: LineMarkerConfig): String

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(
        elements: List<PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        if (elements.isEmpty()) return

        val project = elements.first().project
        val lineMarkerSettings = LineMarkerSettings.getInstance(project)
        val allEnabledConfigs = lineMarkerSettings.getEnabledLineMarkerConfigs()
        val relevantConfigs = filterEnabledConfigs(allEnabledConfigs)

        if (relevantConfigs.isEmpty()) {
            return
        }

        val futures = mutableListOf<Future<MutableList<LineMarkerInfo<*>>>>()
        val executor = AppExecutorUtil.getAppExecutorService()

        elements.asSequence()
            .filter { psiElementClass.isInstance(it) }
            .map {
                @Suppress("UNCHECKED_CAST")
                it as T
            }
            .filter { preFilterElement(it, project) }
            .forEach { typedElement ->
                val callable = Callable {
                    ProgressManager.checkCanceled()
                    val markers = mutableListOf<LineMarkerInfo<*>>()
                    try {
                        ReadAction.run<Throwable> {
                            if (!typedElement.isValid) return@run
                            val anchor = getAnchorElement(typedElement)
                            if (anchor != null && anchor.isValid) {
                                handleElement(project, typedElement, anchor, relevantConfigs, markers)
                            }
                        }
                    } catch (pce: ProcessCanceledException) {
                        throw pce
                    } catch (e: Exception) {
                        val elementDescription = ReadAction.compute<String, Throwable> {
                            if (typedElement.isValid) typedElement.text.take(50) else "invalid element"
                        }
                        thisLogger().error("Error processing element for line marker: $elementDescription", e)
                    }
                    markers
                }
                futures.add(executor.submit(callable))
            }

        for (future in futures) {
            try {
                ProgressManager.checkCanceled()
                result.addAll(future.get())
            } catch (pce: ProcessCanceledException) {
                futures.forEach { if (!it.isDone && !it.isCancelled) it.cancel(true) }
                throw pce
            } catch (e: Exception) {
                thisLogger().error("Error retrieving line marker results from future", e)
            }
        }
    }

    private fun handleElement(
        project: Project,
        originalElement: T,
        anchorElement: PsiElement,
        configs: List<LineMarkerConfig>,
        elementMarkers: MutableList<LineMarkerInfo<*>>
    ) {
        ProgressManager.checkCanceled()
        if (!originalElement.isValid || !anchorElement.isValid) return

        configs.forEach { config ->
            try {
                ProgressManager.checkCanceled()
                val builtQueryResult = ContextAwareQueryBuilder.buildQuery(
                    config.sqlTemplate,
                    originalElement,
                    useDefaultsForUnresolved = false
                )

                builtQueryResult.fold(
                    onSuccess = { finalSql ->
                        val queryResultOutcome = DuckDbService.getInstance(project).executeReadQuery(finalSql)
                        queryResultOutcome.fold(
                            onSuccess = { queryResult ->
                                val metricValue = extractMetricValue(queryResult, finalSql)
                                val displayColor = RuleEvaluator.evaluate(metricValue, config.lineMarkerRules)

                                if (displayColor != null) {
                                    val icon: Icon = ColorIcon(10, displayColor)
                                    val tooltipText = "${config.name}: ${FormattingUtils.formatNumber(metricValue)}"
                                    val tooltipProvider = Function { _: PsiElement? -> tooltipText }

                                    if (!anchorElement.isValid) return@forEach
                                    val info = LineMarkerInfo(
                                        anchorElement,
                                        anchorElement.textRange,
                                        icon,
                                        tooltipProvider,
                                        null,
                                        GutterIconRenderer.Alignment.LEFT,
                                    ) { getLineMarkerGroupName(config) }
                                    elementMarkers.add(info)
                                }
                            },
                            onFailure = { error ->
                                val errorMessage = "DB Error: ${error.message?.take(100)} SQL: ${finalSql.take(100)}"
                                thisLogger().warn("$errorMessage...", error)
                                if (anchorElement.isValid) addErrorMarker(
                                    anchorElement,
                                    config,
                                    errorMessage,
                                    elementMarkers
                                )
                            }
                        )
                    },
                    onFailure = { exception ->
                        val errorMessage = "SQL build failed for '${config.name}': ${exception.message?.take(100)}"
                        thisLogger().trace("$errorMessage...")
                        if (anchorElement.isValid) addErrorMarker(anchorElement, config, errorMessage, elementMarkers)
                    }
                )
            } catch (pce: ProcessCanceledException) {
                throw pce
            } catch (e: Exception) {
                val errorMsgText =
                    ReadAction.compute<String, Throwable> { if (originalElement.isValid) originalElement.text else "invalid element" }
                val errorMessage =
                    "Error for element '${errorMsgText.take(50)}', config '${config.name}': ${e.message?.take(100)}"
                thisLogger().error("$errorMessage...", e)
                if (anchorElement.isValid) addErrorMarker(anchorElement, config, errorMessage, elementMarkers)
            }
        }
    }

    private fun extractMetricValue(queryResult: QueryResult, finalSql: String): Float? {
        return if (queryResult.rows.isNotEmpty() && queryResult.columnNames.isNotEmpty()) {
            try {
                queryResult.rows.first()[queryResult.columnNames.first()].toString().toFloat()
            } catch (e: Exception) {
                thisLogger().warn("Failed to cast metric value to float: ${e.message}. SQL: $finalSql", e)
                null
            }
        } else {
            if (queryResult.rows.isEmpty()) {
                thisLogger().trace("Query returned no rows for single metric value. SQL: $finalSql")
            } else {
                thisLogger().warn("Query returned no columns for single metric value. SQL: $finalSql")
            }
            null
        }
    }

    protected fun addErrorMarker(
        anchorElement: PsiElement,
        config: LineMarkerConfig,
        errorMessage: String,
        elementMarkers: MutableList<in LineMarkerInfo<*>>
    ) {
        if (!anchorElement.isValid) return

        val errorIcon: Icon = IconLoader.getIcon("/icons/breakpointObsolete.svg", this.javaClass)
        val tooltipProvider = { _: PsiElement? ->
            "Error in '${config.name}': $errorMessage"
        }
        val errorInfo = LineMarkerInfo(
            anchorElement,
            anchorElement.textRange,
            errorIcon,
            tooltipProvider,
            null,
            GutterIconRenderer.Alignment.LEFT,
        ) { "Line Marker Error (${config.name})" }
        elementMarkers.add(errorInfo)
    }
}
