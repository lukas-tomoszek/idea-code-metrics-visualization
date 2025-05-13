package com.lukastomoszek.idea.codemetricsvisualization.config.ui.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.lukastomoszek.idea.codemetricsvisualization.config.state.DefaultFeatureEvaluatorConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.FeatureEvaluatorConfig
import com.lukastomoszek.idea.codemetricsvisualization.config.state.FeatureParameterType
import javax.swing.JComponent

class FeatureEvaluatorDialog(
    project: Project,
    config: FeatureEvaluatorConfig,
    existingFeatureEvaluatorNames: List<String>
) : AbstractNamedDialog<FeatureEvaluatorConfig>(project, config, existingFeatureEvaluatorNames) {

    private lateinit var fqnField: JBTextField
    private lateinit var indexSpinner: JBIntSpinner
    private var currentFeatureParameterType: FeatureParameterType = config.featureParameterType

    init {
        title =
            if (config.name == DefaultFeatureEvaluatorConfig.NAME) "Add Feature Evaluator" else "Edit Feature Evaluator"
        init()
    }

    override fun createCenterPanel(): JComponent {

        indexSpinner = JBIntSpinner(config.featureParameterIndex, 0, 10, 1)

        return panel {
            row("Name:") {
                nameField = textField()
                    .bindText(config::name)
                    .validationOnInput { validateName(it.text) }
                    .align(AlignX.FILL).component
            }
            row("Evaluator Method FQN:") {
                fqnField = textField()
                    .bindText(config::evaluatorMethodFqn)
                    .validationOnInput { validateFqn(it.text) }
                    .align(AlignX.FILL).component
            }
            row("Feature Parameter Index:") {
                cell(indexSpinner)
                    .bindIntValue(config::featureParameterIndex)
            }

            buttonsGroup {
                row("Feature Parameter Type:") {
                    val stringRadioButton =
                        radioButton("String (e.g. FeatureEnum.FEATURE1)", FeatureParameterType.STRING)
                            .component
                    stringRadioButton.addActionListener {
                        if (stringRadioButton.isSelected) {
                            currentFeatureParameterType = FeatureParameterType.STRING
                        }
                    }

                    val enumConstantRadioButton =
                        radioButton("Enum constant (e.g. \"feature1\")", FeatureParameterType.ENUM_CONSTANT)
                            .component
                    enumConstantRadioButton.addActionListener {
                        if (enumConstantRadioButton.isSelected) {
                            currentFeatureParameterType = FeatureParameterType.ENUM_CONSTANT
                        }
                    }
                }
            }.bind(config::featureParameterType)
        }
    }

    private fun validateFqn(fqn: String): ValidationInfo? {
        if (fqn.isBlank()) {
            return ValidationInfo("Method FQN cannot be empty", fqnField)
        }
        if (!fqn.contains('.')) {
            return ValidationInfo("Method FQN should be fully qualified (e.g., com.example.Class.method)", fqnField)
        }
        return null
    }
}
