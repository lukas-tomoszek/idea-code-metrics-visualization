/*
 * Copyright (c) 2025 Lukáš Tomoszek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lukastomoszek.idea.codemetricsvisualization.linemarker.provider

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethodCallExpression
import com.lukastomoszek.idea.codemetricsvisualization.config.persistence.FeatureEvaluatorSettings
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.context.PsiUtils
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder

class FeatureEvaluationLineMarkerProvider :
    AbstractMetricLineMarkerProvider<PsiMethodCallExpression>(PsiMethodCallExpression::class.java) {

    override fun filterEnabledConfigs(configs: List<LineMarkerConfig>): List<LineMarkerConfig> {
        return configs.filter { it.sqlTemplate.contains(ContextAwareQueryBuilder.FEATURE_NAME_PLACEHOLDER) }
    }

    override suspend fun preFilterElement(element: PsiMethodCallExpression, project: Project): Boolean {
        val configuredFqns = FeatureEvaluatorSettings.getInstance(project).state.configs
            .map { it.evaluatorMethodFqn }
            .toSet()

        if (configuredFqns.isEmpty()) return false

        val method = readAction { element.resolveMethod() } ?: return false
        val methodFqn = PsiUtils.getContainingMethodFqn(method)

        return methodFqn != null && methodFqn in configuredFqns
    }

    override suspend fun getAnchorElement(element: PsiMethodCallExpression): PsiElement? {
        return readAction {
            element.methodExpression.referenceNameElement ?: element.methodExpression
        }
    }
}
