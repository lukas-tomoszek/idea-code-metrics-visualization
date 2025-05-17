package com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.lukastomoszek.idea.codemetricsvisualization.config.service.LlmPromptGenerationService
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ChartConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DefaultChartConfig
import javax.swing.JButton
import javax.swing.JComponent

class ChartDialog(
    private val project: Project,
    config: ChartConfig,
    existingChartNames: List<String>
) : AbstractNamedDialog<ChartConfig>(project, config, existingChartNames) {

    private lateinit var sqlTextArea: JBTextArea
    private lateinit var llmDescriptionTextArea: JBTextArea
    private lateinit var llmRelevantTableNamesField: JBTextField
    private lateinit var selectTablesButton: JButton
    private lateinit var copyLlmButton: JButton

    private var currentLlmRelevantTableNames: MutableList<String> = config.llmRelevantTableNames.toMutableList()

    init {
        title = if (config.name == DefaultChartConfig.NAME) "Add Chart Configuration" else "Edit Chart Configuration"
        init()
    }

    override fun createCenterPanel(): JComponent {
        sqlTextArea = JBTextArea(10, 70).apply {
            text = config.sqlTemplate
            lineWrap = true
            wrapStyleWord = true
        }

        llmDescriptionTextArea = JBTextArea(3, 70).apply {
            text = config.llmDescription
            lineWrap = true
            wrapStyleWord = true
        }

        llmRelevantTableNamesField = JBTextField().apply {
            text = formatTableNamesForDisplay(currentLlmRelevantTableNames)
            isEditable = false
        }

        selectTablesButton = JButton("Select Tables...").apply {
            addActionListener {
                val dialog = SelectTablesDialog(project, currentLlmRelevantTableNames.toList())
                if (dialog.showAndGet()) {
                    currentLlmRelevantTableNames.clear()
                    currentLlmRelevantTableNames.addAll(dialog.getSelectedTableNames())
                    llmRelevantTableNamesField.text = formatTableNamesForDisplay(currentLlmRelevantTableNames)
                }
            }
        }

        return panel {
            group("Basic Info") {
                row("Name:") {
                    nameField = textField()
                        .bindText(config::name)
                        .validationOnInput { validateName(it.text) }
                        .align(AlignX.FILL)
                        .component
                }
            }

            group("LLM Integration") {
                row {
                    label("LLM description:")
                }
                row {
                    cell(JBScrollPane(llmDescriptionTextArea))
                        .align(Align.FILL)
                }.resizableRow()

                row("LLM Relevant Tables:") {
                    cell(llmRelevantTableNamesField)
                        .resizableColumn()
                        .align(AlignX.FILL)
                    cell(selectTablesButton)
                }

                row {
                    copyLlmButton = JButton("Copy LLM Prompt for SQL Generation").apply {
                        addActionListener {
                            val currentConfig = getUpdatedConfigFromForm()
                            LlmPromptGenerationService.getInstance(project)
                                .generateChartSqlPrompt(currentConfig)
                        }
                    }
                    cell(copyLlmButton)
                        .align(AlignX.FILL)
                        .comment(
                            "Copies a prompt with instructions and samples of selected tables to your clipboard for use with an AI tool. " +
                            "Review the content before use, as it may contain private or sensitive data.",
                            100
                        )
                }
            }

            group("SQL Template:") {
                row {
                    cell(JBScrollPane(sqlTextArea))
                        .align(Align.FILL)
                }.resizableRow()
            }
        }
    }

    private fun formatTableNamesForDisplay(tableNames: List<String>): String {
        return if (tableNames.isEmpty()) "None selected" else tableNames.joinToString(", ")
    }

    private fun getUpdatedConfigFromForm(): ChartConfig {
        return config.copy(
            name = nameField.text,
            sqlTemplate = sqlTextArea.text,
            llmDescription = llmDescriptionTextArea.text,
            llmRelevantTableNames = currentLlmRelevantTableNames.toList()
        )
    }

    override fun doOKAction() {
        config.sqlTemplate = sqlTextArea.text
        config.llmDescription = llmDescriptionTextArea.text
        config.llmRelevantTableNames = currentLlmRelevantTableNames.toList()
        super.doOKAction()
    }
}
