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
