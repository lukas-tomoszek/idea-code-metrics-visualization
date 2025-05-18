package com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.lukastomoszek.idea.codemetricsvisualization.config.service.LlmPromptGenerationService
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DefaultLineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.linemarker.rule.RangeRule
import com.lukastomoszek.idea.codemetricsvisualization.linemarker.rule.RuleEvaluator
import javax.swing.JButton
import javax.swing.JComponent

class LineMarkerDialog(
    private val project: Project,
    config: LineMarkerConfig,
    existingLineMarkerNames: List<String>
) : AbstractNamedDialog<LineMarkerConfig>(project, config, existingLineMarkerNames) {

    private lateinit var sqlTextArea: JBTextArea
    private lateinit var rulesTextArea: JBTextArea
    private val rules = config.lineMarkerRules.toMutableList()
    private lateinit var llmDescriptionTextArea: JBTextArea
    private lateinit var llmRelevantTableNamesField: JBTextField
    private lateinit var selectTablesButton: JButton
    private lateinit var copyLlmButton: JButton

    private var currentLlmRelevantTableNames: MutableList<String> = config.llmRelevantTableNames.toMutableList()

    init {
        title =
            if (config.name == DefaultLineMarkerConfig.NAME) "Add Line Marker Configuration" else "Edit Line Marker Configuration"
        init()
    }

    override fun createCenterPanel(): JComponent {
        sqlTextArea = JBTextArea(5, 60).apply {
            text = config.sqlTemplate
            lineWrap = true
            wrapStyleWord = true
        }

        rulesTextArea = JBTextArea(5, 60).apply {
            text = rules.joinToString("\n") {
                "${it.fromString};${it.toString};${it.colorHex}"
            }
            lineWrap = false
            toolTipText =
                "Each rule on a new line: from_exclusive_or_empty;to_inclusive_or_empty;hex_color_or_empty\n" +
                "Empty means unbounded (i.e., -∞ or +∞) or no color.\n" +
                "Rules are evaluated top-down; the first match applies.\n\n" +
                "Examples for percentage:\n;50;#FF0000\n50;75;#FFFF00\n75;;#00FF00\n" +
                "Examples for error count:\n;0;#00FF00\n0;5;#FFFF00\n5;;#FF0000"
        }

        llmDescriptionTextArea = JBTextArea(3, 60).apply {
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
                row("LLM Description:") {}
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
                    copyLlmButton = JButton("Copy LLM Prompt for SQL & Rules").apply {
                        addActionListener {
                            val currentConfig = getUpdatedConfigFromForm()
                            LlmPromptGenerationService.getInstance(project)
                                .generateLineMarkerSqlPrompt(currentConfig)
                        }
                    }
                    cell(copyLlmButton)
                        .align(AlignX.FILL)
                        .comment(
                            "Copies a prompt with instructions and samples of the selected tables to your clipboard for use with an AI tool. " +
                            "Review the content before use, as it may contain private or sensitive data.",
                            100
                        )
                }
            }

            group("Line Marker Logic") {
                row("SQL template:") {}
                row {
                    cell(JBScrollPane(sqlTextArea))
                        .align(Align.FILL)
                        .validationOnApply { validateSqlTemplate() }
                        .validationRequestor {
                            sqlTextArea.document.addDocumentListener(object : javax.swing.event.DocumentListener {
                                override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = it()
                                override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = it()
                                override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = it()
                            })
                        }
                }.resizableRow()

                row("Coloring Rules (from_exclusive;to_inclusive;color_hex_or_null):") {}

                row {
                    cell(JBScrollPane(rulesTextArea))
                        .align(Align.FILL)
                        .validationOnApply { validateRules() }
                        .validationRequestor {
                            rulesTextArea.document.addDocumentListener(object : javax.swing.event.DocumentListener {
                                override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = it()
                                override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = it()
                                override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = it()
                            })
                        }
                }.resizableRow()
            }
        }
    }

    fun validateSqlTemplate() =
        sqlTextArea.text.takeIf { it.isNotBlank() && '#' !in it }
            ?.let { ValidationInfo("SQL template must contain a placeholder (e.g., #method_name#).", sqlTextArea) }

    fun validateRules(): ValidationInfo? {
        val ruleLines = rulesTextArea.text.split('\n').filter { it.isNotBlank() }
        for ((index, line) in ruleLines.withIndex()) {
            val parts = line.split(';', limit = 3)
            if (parts.size != 3) {
                return ValidationInfo(
                    "Rule on line ${index + 1} is malformed: '$line'. Expected from;to;color format.",
                    rulesTextArea
                )
            }
            val fromStr = parts[0].trim()
            val toStr = parts[1].trim()
            val colorStr = parts[2].trim()

            try {
                RuleEvaluator.parseBoundaryString(fromStr, true)
            } catch (e: NumberFormatException) {
                return ValidationInfo(
                    "Invalid 'from' value on line ${index + 1}: '$fromStr'. Must be a number or empty.",
                    rulesTextArea
                )
            }

            try {
                RuleEvaluator.parseBoundaryString(toStr, false)
            } catch (e: NumberFormatException) {
                return ValidationInfo(
                    "Invalid 'to' value on line ${index + 1}: '$fromStr'. Must be a number or empty.",
                    rulesTextArea
                )
            }

            if (colorStr.isNotEmpty()) {
                if (!Regex("^#([0-9a-fA-F]{6})$").matches(colorStr)) {
                    return ValidationInfo(
                        "Invalid color hex on line ${index + 1}: '$colorStr'. Expected #RRGGBB or empty.",
                        rulesTextArea
                    )
                }
            }
        }
        return null
    }

    private fun formatTableNamesForDisplay(tableNames: List<String>): String {
        return if (tableNames.isEmpty()) "None selected" else tableNames.joinToString(", ")
    }

    private fun getUpdatedConfigFromForm(): LineMarkerConfig {
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

        val parsedRules = mutableListOf<RangeRule>()
        val ruleLines = rulesTextArea.text.split('\n').filter { it.isNotBlank() }
        for (line in ruleLines) {
            val parts = line.split(';', limit = 3)
            parsedRules.add(
                RangeRule(
                    fromString = parts[0].trim(),
                    toString = parts[1].trim(),
                    colorHex = parts[2].trim().uppercase()
                )
            )
        }
        config.lineMarkerRules.clear()
        config.lineMarkerRules.addAll(parsedRules)

        super.doOKAction()
    }
}
