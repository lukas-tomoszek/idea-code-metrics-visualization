package com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import com.lukastomoszek.idea.codemetricsvisualization.config.service.LlmPromptGenerationService
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DefaultLineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerOperator
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerRule
import javax.swing.DefaultCellEditor
import javax.swing.JButton
import javax.swing.JComponent

class LineMarkerDialog(
    private val project: Project,
    config: LineMarkerConfig,
    existingLineMarkerNames: List<String>
) : AbstractNamedDialog<LineMarkerConfig>(project, config, existingLineMarkerNames) {

    private lateinit var sqlTextArea: JBTextArea
    private lateinit var rulesTableModel: ListTableModel<LineMarkerRule>
    private val rules = config.lineMarkerRules.map { it.copy() }.toMutableList()
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

        val operatorColumn = object : ColumnInfo<LineMarkerRule, LineMarkerOperator>("Operator") {
            override fun valueOf(item: LineMarkerRule): LineMarkerOperator = item.operator
            override fun setValue(item: LineMarkerRule, value: LineMarkerOperator) {
                item.operator = value
            }

            override fun isCellEditable(item: LineMarkerRule) = true

            override fun getEditor(item: LineMarkerRule): DefaultCellEditor {
                val comboBox = ComboBox(EnumComboBoxModel(LineMarkerOperator::class.java))
                return DefaultCellEditor(comboBox)
            }
        }

        val thresholdColumn = object : ColumnInfo<LineMarkerRule, String>("Threshold (Float)") {
            override fun valueOf(item: LineMarkerRule): String = item.threshold.toString()
            override fun setValue(item: LineMarkerRule, value: String) {
                item.threshold = value.toFloatOrNull() ?: item.threshold
            }

            override fun isCellEditable(item: LineMarkerRule) = true
        }

        val colorColumn = object : ColumnInfo<LineMarkerRule, String>("Color (Hex or Blank)") {
            override fun valueOf(item: LineMarkerRule): String = item.colorHex ?: ""
            override fun setValue(item: LineMarkerRule, value: String) {
                val trimmedValue = value.trim()
                if (trimmedValue.isEmpty()) {
                    item.colorHex = null
                } else {
                    val hexValue = if (trimmedValue.startsWith("#")) trimmedValue else "#$trimmedValue"
                    if (Regex("^#[0-9a-fA-F]{6}$").matches(hexValue)) {
                        item.colorHex = hexValue.uppercase()
                    }
                }
            }

            override fun isCellEditable(item: LineMarkerRule) = true
        }

        rulesTableModel = ListTableModel(arrayOf(operatorColumn, thresholdColumn, colorColumn), rules, 0)
        val rulesTable = JBTable(rulesTableModel).apply {
            setShowGrid(true)
            emptyText.text = "No rules defined"

            val opColumnModel = getColumnModel().getColumn(0)
            opColumnModel.cellEditor = operatorColumn.getEditor(LineMarkerRule())
            opColumnModel.cellRenderer = operatorColumn.getRenderer(LineMarkerRule())
        }

        val rulesToolbar = ToolbarDecorator.createDecorator(rulesTable)
            .setAddAction { addRule() }
            .setRemoveAction { removeRule(rulesTable.selectedRow) }
            .createPanel()

        return panel {
            row("Name:") {
                nameField = textField()
                    .bindText(config::name)
                    .validationOnInput { validateName(it.text) }
                    .align(AlignX.FILL)
                    .component
            }

            row {
                label("LLM Description:")
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
                            .generateLineMarkerSqlPrompt(currentConfig)
                    }
                }
                cell(copyLlmButton)
                    .align(AlignX.FILL)
                    .comment(
                        "Copies a prompt with instructions and samples of selected tables to your clipboard for use with an AI tool. Review the content before use, as it may contain private or sensitive data.",
                        100
                    )
            }

            row {
                label("SQL template:")
            }
            row {
                cell(JBScrollPane(sqlTextArea))
                    .align(Align.FILL)
            }.resizableRow()

            row {
                label("Rules:")
            }
            row {
                cell(rulesToolbar).align(Align.FILL)
            }.resizableRow()

        }
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

    private fun addRule() {
        rules.add(LineMarkerRule())
        rulesTableModel.fireTableDataChanged()
    }

    private fun removeRule(selectedRow: Int) {
        if (selectedRow >= 0) {
            rules.removeAt(selectedRow)
            rulesTableModel.fireTableDataChanged()
        }
    }

    override fun doOKAction() {
        config.sqlTemplate = sqlTextArea.text
        config.llmDescription = llmDescriptionTextArea.text
        config.llmRelevantTableNames = currentLlmRelevantTableNames.toList()
        config.lineMarkerRules = rules
        super.doOKAction()
    }
}
