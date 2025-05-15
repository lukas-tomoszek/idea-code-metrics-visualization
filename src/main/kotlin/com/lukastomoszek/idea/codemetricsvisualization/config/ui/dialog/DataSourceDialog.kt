package com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.*
import com.lukastomoszek.idea.codemetricsvisualization.config.service.LlmPromptGenerationService
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DataSourceConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DefaultDataSource
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ImportMode
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.JButton
import javax.swing.JComponent

class DataSourceDialog(
    private val project: Project,
    config: DataSourceConfig,
    existingDataSourceNames: List<String>
) : AbstractNamedDialog<DataSourceConfig>(project, config, existingDataSourceNames) {

    private lateinit var filePathField: TextFieldWithBrowseButton
    private lateinit var sqlTextArea: JBTextArea
    private lateinit var llmAdditionalInfoTextArea: JBTextArea
    private var currentImportMode: ImportMode = config.importMode

    private lateinit var copyLlmButton: JButton
    private val placeholder = """
        Optional: Provide extra context or specific instructions to guide SQL generation.
        For example:
        - The CSV uses '~' as delimiter.
        - Timestamps are in 'Unix epoch timestamp' format or in 'dd/MM/yyyy @ HH:mm:ss' format.
        - Truncate time from timestamps and store them as DATE to save space.
        - Only import the following columns: "timestamp", "user_id", "method_fqn".
        - Do not rename columns unless necessary. / Rename columns to meaningful names.
        - Rename column 'untitled' to 'method_name'.
        - Cast 'transaction_amount' to DECIMAL(10,2).
        - Exclude rows where 'status' = 'test_order'.
        - If appending, the target table already has columns: timestamp (DATE), status_code (VARCHAR), mapping_path (VARCHAR).
    """.trimIndent()

    private fun JBTextArea.cleanedTextOrNull(): String =
        text.takeIf { it != placeholder } ?: ""

    init {
        title =
            if (config.name == DefaultDataSource.NAME) "Add Data Source Configuration" else "Edit Data Source Configuration"
        init()
    }

    override fun createCenterPanel(): JComponent {
        filePathField = TextFieldWithBrowseButton().apply {
            addBrowseFolderListener(
                project,
                FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor()
            )
        }

        llmAdditionalInfoTextArea = JBTextArea(5, 100).apply {
            text = config.llmAdditionalInfo.ifBlank { placeholder }
            lineWrap = true
            wrapStyleWord = true
            foreground = if (text == placeholder) JBColor.GRAY else JBColor.foreground()

            addFocusListener(object : FocusAdapter() {
                override fun focusGained(e: FocusEvent) {
                    if (text == placeholder) {
                        text = ""
                        foreground = JBColor.foreground()
                    }
                }

                override fun focusLost(e: FocusEvent) {
                    if (text.isBlank()) {
                        text = placeholder
                        foreground = JBColor.GRAY
                    }
                }
            })
        }

        sqlTextArea = JBTextArea(10, 100).apply {
            text = config.sql
            lineWrap = true
            wrapStyleWord = true
        }

        return panel {
            row("Name:") {
                nameField = textField()
                    .bindText(config::name)
                    .validationOnInput { validateName(it.text) }
                    .align(AlignX.FILL)
                    .component
            }

            row("Table Name:") {
                textField()
                    .bindText(config::tableName)
                    .validationOnInput { if (it.text.isBlank()) error("Table name cannot be empty") else null }
                    .align(AlignX.FILL)
                    .component
            }

            row("File Path:") {
                cell(filePathField)
                    .bindText(config::filePath)
                    .align(AlignX.FILL)
            }

            row {
                label("Additional information for LLM:")
            }
            row {
                cell(JBScrollPane(llmAdditionalInfoTextArea))
                    .align(Align.FILL)
            }.resizableRow()


            buttonsGroup {
                row("Import Mode:") {
                    val replaceRadioButton = radioButton("Replace existing table", ImportMode.REPLACE)
                        .component
                    replaceRadioButton.addActionListener {
                        if (replaceRadioButton.isSelected) {
                            currentImportMode = ImportMode.REPLACE
                        }
                    }

                    val appendRadioButton = radioButton("Append to existing table data", ImportMode.APPEND)
                        .component
                    appendRadioButton.addActionListener {
                        if (appendRadioButton.isSelected) {
                            currentImportMode = ImportMode.APPEND
                        }
                    }
                }
            }.bind(config::importMode)


            row {
                copyLlmButton = JButton("Copy LLM Prompt for SQL Generation").apply {
                    addActionListener {
                        LlmPromptGenerationService.getInstance(project)
                            .generateDataSourceImportPrompt(
                                config.copy(
                                    sql = sqlTextArea.text,
                                    llmAdditionalInfo = llmAdditionalInfoTextArea.cleanedTextOrNull()
                                )
                            )
                    }
                }
                cell(copyLlmButton)
                    .align(AlignX.FILL)
                    .comment(
                        "Copies a prompt with instructions and a sample of the selected file to your clipboard for use with an AI tool. Review the content before use, as it may contain private or sensitive data.",
                        100
                    )
            }

            row {
                label("Import SQL:")
            }

            row {
                cell(JBScrollPane(sqlTextArea))
                    .align(Align.FILL)
            }.resizableRow()
        }
    }

    override fun doOKAction() {
        config.sql = sqlTextArea.text
        config.llmAdditionalInfo = llmAdditionalInfoTextArea.cleanedTextOrNull()
        super.doOKAction()
    }
}
