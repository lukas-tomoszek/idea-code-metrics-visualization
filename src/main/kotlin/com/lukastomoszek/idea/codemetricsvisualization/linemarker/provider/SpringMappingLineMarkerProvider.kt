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
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.lukastomoszek.idea.codemetricsvisualization.config.state.LineMarkerConfig
import com.lukastomoszek.idea.codemetricsvisualization.context.SpringMappingExtractionUtil
import com.lukastomoszek.idea.codemetricsvisualization.db.ContextAwareQueryBuilder

class SpringMappingLineMarkerProvider : AbstractMetricLineMarkerProvider<PsiAnnotation>(PsiAnnotation::class.java) {

    override fun filterEnabledConfigs(configs: List<LineMarkerConfig>): List<LineMarkerConfig> {
        return configs.filter {
            (it.sqlTemplate.contains(ContextAwareQueryBuilder.MAPPING_PATH_PLACEHOLDER)
             || it.sqlTemplate.contains(ContextAwareQueryBuilder.MAPPING_METHOD_PLACEHOLDER))
            && !it.sqlTemplate.contains(ContextAwareQueryBuilder.FEATURE_NAME_PLACEHOLDER)
        }
    }

    override suspend fun preFilterElement(element: PsiAnnotation, project: Project): Boolean {
        return readAction { SpringMappingExtractionUtil.isSpringMappingAnnotation(element.qualifiedName) }
    }

    override suspend fun getAnchorElement(element: PsiAnnotation): PsiElement? = readAction {
        element.nameReferenceElement?.referenceNameElement ?: element.nameReferenceElement ?: element
    }
}
