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
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.ColorIcon
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.LineMarkerSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder
import com.lukastomoszek.idea.codemetricsvisualization.db.DuckDbService
import com.lukastomoszek.idea.codemetricsvisualization.linemarker.rule.RuleEvaluator
import java.util.concurrent.Callable
import java.util.concurrent.Future
import javax.swing.Icon

class MethodDefinitionLineMarkerProvider : LineMarkerProvider, DumbAware {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(
        elements: List<PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        if (elements.isEmpty()) return

        val project = elements.first().project
        val settings = LineMarkerSettings.getInstance(project)

        val enabledConfigs = settings.getEnabledLineMarkerConfigs().filter {
            it.sqlTemplate.contains(ContextAwareQueryBuilder.METHOD_NAME_PLACEHOLDER)
        }

        if (enabledConfigs.isEmpty()) {
            return
        }

        val futures = mutableListOf<Future<MutableList<LineMarkerInfo<*>>>>()
        val executor = AppExecutorUtil.getAppExecutorService()

        elements.asSequence()
            .mapNotNull { (it as? PsiIdentifier)?.parent as? PsiMethod }
            .distinct()
            .forEach { method ->
                val callable = Callable {
                    ProgressManager.checkCanceled()
                    val markersForElement = mutableListOf<LineMarkerInfo<*>>()
                    try {
                        ReadAction.run<Throwable> {
                            handleMethodDefinition(
                                project,
                                method,
                                method.nameIdentifier,
                                enabledConfigs,
                                markersForElement
                            )
                        }
                    } catch (pce: ProcessCanceledException) {
                        throw pce
                    } catch (e: Exception) {
                        thisLogger().error(
                            "Error creating line marker for method ${method.name}",
                            e
                        )
                    }
                    markersForElement
                }
                futures.add(executor.submit(callable))
            }

        val allGeneratedLineMarkers = mutableListOf<LineMarkerInfo<*>>()
        for (future in futures) {
            try {
                ProgressManager.checkCanceled()
                allGeneratedLineMarkers.addAll(future.get())
            } catch (pce: ProcessCanceledException) {
                futures.forEach { it.cancel(true) }
                throw pce
            } catch (e: Exception) {
                thisLogger().error("Error retrieving line marker results from future for MethodDefinition", e)
            }
        }
        result.addAll(allGeneratedLineMarkers)
    }

    private fun handleMethodDefinition(
        project: Project,
        method: PsiMethod,
        anchorElement: PsiIdentifier?,
        configs: List<LineMarkerConfig>,
        elementMarkers: MutableList<LineMarkerInfo<*>>
    ) {
        if (anchorElement == null) return
        ProgressManager.checkCanceled()

        configs.forEach { config ->
            try {
                ProgressManager.checkCanceled()
                val builtQueryResult =
                    ContextAwareQueryBuilder.buildQuery(config.sqlTemplate, anchorElement, useDefaults = false)

                builtQueryResult.fold(
                    onSuccess = { finalSql ->
                        val queryResultOutcome = DuckDbService.getInstance(project).executeReadQuery(finalSql)
                        queryResultOutcome.fold(
                            onSuccess = { queryResult ->
                                val metricValue: Float? =
                                    if (queryResult.rows.isNotEmpty() && queryResult.columnNames.isNotEmpty()) {
                                        try {
                                            queryResult.rows.first()[queryResult.columnNames.first()].toString()
                                                .toFloat()
                                        } catch (e: Exception) {
                                            thisLogger().warn(
                                                "Failed to cast metric value to float: ${e.message}. SQL: $finalSql",
                                                e
                                            )
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

                                val displayColor = RuleEvaluator.evaluate(metricValue, config.lineMarkerRules)
                                if (displayColor != null) {
                                    val icon: Icon = ColorIcon(10, displayColor)
                                    val tooltipProvider = com.intellij.util.Function { _: PsiElement? ->
                                        val valueStr = metricValue?.let {
                                            if (it % 1 == 0.0F) String.format(
                                                "%.0f",
                                                it
                                            ) else String.format("%.2f", it)
                                        } ?: "N/A"
                                        "${config.name}: $valueStr"
                                    }
                                    val info = LineMarkerInfo(
                                        anchorElement,
                                        anchorElement.textRange,
                                        icon,
                                        tooltipProvider,
                                        null,
                                        GutterIconRenderer.Alignment.LEFT
                                    ) { "Code Metric Visualization (${config.name})" }
                                    elementMarkers.add(info)
                                }
                            },
                            onFailure = { error ->
                                val errorMessage =
                                    "DB Error: ${error.message} SQL: $finalSql"
                                thisLogger().warn(errorMessage, error)
                                addErrorMarker(anchorElement, config, errorMessage, elementMarkers)
                            }
                        )
                    },
                    onFailure = { exception ->
                        val errorMessage = "SQL build failed for '${config.name}': ${exception.message}"
                        thisLogger().trace(errorMessage)
                        addErrorMarker(anchorElement, config, errorMessage, elementMarkers)
                    }
                )
            } catch (pce: ProcessCanceledException) {
                throw pce
            } catch (e: Exception) {
                val errorMessage =
                    "Error for method '${method.name}', config '${config.name}': ${e.message}"
                thisLogger().error(errorMessage, e)
                addErrorMarker(anchorElement, config, errorMessage, elementMarkers)
            }
        }
    }

    private fun addErrorMarker(
        anchorElement: PsiElement,
        config: LineMarkerConfig,
        errorMessage: String,
        elementMarkers: MutableList<in LineMarkerInfo<*>>
    ) {
        val errorIcon: Icon =
            IconLoader.getIcon("/icons/breakpointObsolete.svg", MethodDefinitionLineMarkerProvider::class.java)
        val tooltipProvider = { _: PsiElement? ->
            "Error in '${config.name}': $errorMessage"
        }
        val errorInfo = LineMarkerInfo(
            anchorElement,
            anchorElement.textRange,
            errorIcon,
            tooltipProvider,
            null,
            GutterIconRenderer.Alignment.LEFT
        ) { "Line Marker Error (${config.name})" }
        elementMarkers.add(errorInfo)
    }
}
