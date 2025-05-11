package com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.lukastomoszek.idea.codemetricsvisualization.config.state.ChartConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DefaultChartConfig
import javax.swing.JComponent

class ChartDialog(
    project: Project,
    private val config: ChartConfig,
    private val existingChartNames: List<String>
) : AbstractDialog<ChartConfig>(project) {

    private lateinit var nameField: com.intellij.ui.components.JBTextField
    private lateinit var sqlTextArea: JBTextArea
    private val originalName = config.name

    init {
        title = if (config.name == DefaultChartConfig.NAME) "Add Chart Configuration" else "Edit Chart Configuration"
        init()
    }

    override fun createCenterPanel(): JComponent {
        sqlTextArea = JBTextArea(10, 70).apply {
            text = config.sql
            lineWrap = true
            wrapStyleWord = true
        }

        return panel {
            row("Name:") {
                nameField = textField()
                    .bindText(config::name)
                    .validationOnInput {
                        if (it.text.isBlank()) error("Name cannot be empty")
                        else if (it.text != originalName && existingChartNames.any { name ->
                                name.equals(
                                    it.text,
                                    ignoreCase = true
                                )
                            }) error("A chart with this name already exists")
                        else null
                    }
                    .align(AlignX.FILL)
                    .component
            }

            row {
                label("SQL Query:")
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

    override fun getUpdatedConfig(): ChartConfig = config
}
