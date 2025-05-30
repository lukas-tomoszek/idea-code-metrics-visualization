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

package com.lukastomoszek.idea.codemetricsvisualization.testutils

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.junit.Assert.assertTrue

fun PsiFile.getElementAtMarker(marker: String): PsiElement {
    val index = text.indexOf(marker)
    assertTrue("Marker '$marker' not found in file $name", index != -1)
    return findElementAt(index + marker.length)
           ?: error("Element at marker '$marker' should not be null")
}
