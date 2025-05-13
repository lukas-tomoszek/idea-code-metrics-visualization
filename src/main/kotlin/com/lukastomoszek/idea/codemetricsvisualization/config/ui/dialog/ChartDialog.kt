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
    config: ChartConfig,
    existingChartNames: List<String>
) : AbstractNamedDialog<ChartConfig>(project, config, existingChartNames) {

    private lateinit var sqlTextArea: JBTextArea

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
                    .validationOnInput { validateName(it.text) }
                    .align(AlignX.FILL)
                    .component
            }

            row {
                label("SQL query:")
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
