package com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DataSourceConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ImportMode
import javax.swing.JComponent

class DataSourceDialog(private val project: Project, private val config: DataSourceConfig) :
    AbstractDialog<DataSourceConfig>(project) {

    private lateinit var nameField: JBTextField
    private lateinit var tableNameField: JBTextField
    private lateinit var filePathField: TextFieldWithBrowseButton
    private lateinit var sqlTextArea: JBTextArea
    private var currentImportMode: ImportMode = config.importMode

    init {
        title =
            if (config.name == "New Data Source") "Add Data Source Configuration" else "Edit Data Source Configuration"
        init()
    }

    override fun createCenterPanel(): JComponent {
        sqlTextArea = JBTextArea(20, 100).apply {
            text = config.sql
            lineWrap = true
            wrapStyleWord = true
        }

        filePathField = TextFieldWithBrowseButton().apply {
            addBrowseFolderListener(
                project,
                FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor()
            )
            text = config.filePath
        }

        return panel {
            row("Name:") {
                nameField = textField()
                    .bindText(config::name)
                    .validationOnInput { if (it.text.isBlank()) error("Name cannot be empty") else null }
                    .align(AlignX.FILL)
                    .component
            }

            row("Table Name:") {
                tableNameField = textField()
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
                label("Import SQL:")
            }

            row {
                cell(JBScrollPane(sqlTextArea))
                    .align(Align.FILL)
            }.resizableRow()
        }
    }

    override fun doOKAction() {
        config.name = nameField.text
        config.tableName = tableNameField.text
        config.filePath = filePathField.text
        config.importMode = currentImportMode
        config.sql = sqlTextArea.text
        super.doOKAction()
    }

    override fun getUpdatedConfig(): DataSourceConfig = config
}
