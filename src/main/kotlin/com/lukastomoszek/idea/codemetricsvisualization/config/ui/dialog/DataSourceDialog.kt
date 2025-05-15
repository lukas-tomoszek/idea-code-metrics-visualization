package com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.*
import com.lukastomoszek.idea.codemetricsvisualization.config.service.LlmPromptGenerationService
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DataSourceConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DefaultDataSource
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ImportMode
import javax.swing.JButton
import javax.swing.JComponent

class DataSourceDialog(
    private val project: Project,
    config: DataSourceConfig,
    existingDataSourceNames: List<String>
) : AbstractNamedDialog<DataSourceConfig>(project, config, existingDataSourceNames) {

    private lateinit var filePathField: TextFieldWithBrowseButton
    private lateinit var sqlTextArea: JBTextArea
    private lateinit var copyLlmButton: JButton
    private var currentImportMode: ImportMode = config.importMode

    init {
        title =
            if (config.name == DefaultDataSource.NAME) "Add Data Source Configuration" else "Edit Data Source Configuration"
        init()
    }

    override fun createCenterPanel(): JComponent {
        sqlTextArea = JBTextArea(15, 100).apply {
            text = config.sql
            lineWrap = true
            wrapStyleWord = true
        }

        filePathField = TextFieldWithBrowseButton().apply {
            addBrowseFolderListener(
                project,
                FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor()
            )
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
                            .generateDataSourceImportPrompt(config.copy(sql = sqlTextArea.text))
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
        super.doOKAction()
    }
}
