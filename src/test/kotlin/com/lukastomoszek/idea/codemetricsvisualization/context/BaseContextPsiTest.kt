package com.lukastomoszek.idea.codemetricsvisualization.context

import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.lukastomoszek.idea.codemetricsvisualization.testutils.TEST_DATA_PATH
import com.lukastomoszek.idea.codemetricsvisualization.testutils.clearFeatureEvaluatorSettings
import com.lukastomoszek.idea.codemetricsvisualization.testutils.getElementAtMarker

abstract class BaseContextPsiTest : BasePlatformTestCase() {
    override fun getTestDataPath() = TEST_DATA_PATH

    override fun setUp() {
        super.setUp()
        myFixture.copyDirectoryToProject("psi/util", "")
    }

    override fun tearDown() {
        try {
            clearProjectSettings()
        } finally {
            super.tearDown()
        }
    }

    protected open fun clearProjectSettings() = clearFeatureEvaluatorSettings(project)

    protected fun loadElement(filePath: String, marker: String): PsiElement {
        val psiFile = myFixture.configureByFile(filePath)
        return psiFile.getElementAtMarker(marker)
    }
}
