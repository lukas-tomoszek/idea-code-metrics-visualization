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
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder

class MethodDefinitionLineMarkerProvider : AbstractMetricLineMarkerProvider<PsiIdentifier>(PsiIdentifier::class.java) {

    override fun filterEnabledConfigs(configs: List<LineMarkerConfig>): List<LineMarkerConfig> {
        return configs.filter {
            it.sqlTemplate.contains(ContextAwareQueryBuilder.METHOD_FQN_PLACEHOLDER)
            && !it.sqlTemplate.contains(ContextAwareQueryBuilder.FEATURE_NAME_PLACEHOLDER)
            && !it.sqlTemplate.contains(ContextAwareQueryBuilder.MAPPING_PATH_PLACEHOLDER)
            && !it.sqlTemplate.contains(ContextAwareQueryBuilder.MAPPING_METHOD_PLACEHOLDER)
        }
    }

    override suspend fun preFilterElement(element: PsiIdentifier, project: Project): Boolean {
        return readAction {
            (element.parent as? PsiMethod)
                ?.takeIf { it.nameIdentifier == element }
                ?.containingClass
                ?.let { it !is PsiAnonymousClass } ?: false
        }
    }

    override suspend fun getAnchorElement(element: PsiIdentifier): PsiElement? {
        return readAction {
            (element.parent as? PsiMethod)?.nameIdentifier
        }
    }
}
