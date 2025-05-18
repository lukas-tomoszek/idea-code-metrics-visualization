package com.lukastomoszek.idea.codemetricsvisualization.context

import com.lukastomoszek.idea.codemetricsvisualization.testutils.FeatureEvaluatorTestConfigs.enumEvaluator
import com.lukastomoszek.idea.codemetricsvisualization.testutils.FeatureEvaluatorTestConfigs.stringEvaluator
import com.lukastomoszek.idea.codemetricsvisualization.testutils.getElementAtMarker
import com.lukastomoszek.idea.codemetricsvisualization.testutils.setupFeatureEvaluatorSettings
import kotlinx.coroutines.runBlocking

class PsiFileUtilsTest : BaseContextPsiTest() {

    override fun setUp() {
        super.setUp()
        setupFeatureEvaluatorSettings(project, listOf(stringEvaluator, enumEvaluator))
    }

    fun testGetMethodsAndFeaturesInContainingFile_Controller() = runBlocking {
        val element = loadElement("psi/file/ControllerForFileUtils.java", "/* FILE_UTILS_METHOD_ONE_MARKER */")
        val (methods, features) = PsiFileUtils.getMethodsAndFeaturesInContainingFile(element)

        assertContainsElements(
            methods,
            "com.example.fileutils.ControllerForFileUtils.methodOneFileUtils",
            "com.example.fileutils.ControllerForFileUtils.methodTwoFileUtils",
            "com.example.fileutils.ControllerForFileUtils.utilityMethodFileUtils",
            "com.example.fileutils.ControllerForFileUtils.classLevelPathMethodFileUtils",
            "com.example.fileutils.ControllerForFileUtils.methodThreeFileUtils",
        )
        assertEquals(5, methods.size)

        assertContainsElements(
            features,
            "featureX.fileutils",
            "featureY.fileutils",
            "DISABLED_FEATURE",
            "ANOTHER_FEATURE"
        )
        assertEquals(4, features.size)
    }

    fun testGetMethodsAndFeaturesInContainingFile_OnlyFeatures() = runBlocking {
        val element = loadElement("psi/file/OnlyFeaturesFileUtils.java", "/* FILE_UTILS_ONLY_FEATURES_CLASS_MARKER */")
        val (methods, features) = PsiFileUtils.getMethodsAndFeaturesInContainingFile(element)

        assertContainsElements(
            methods,
            "com.example.fileutils.OnlyFeaturesFileUtils.triggerFeaturesFileUtils",
        )
        assertEquals(1, methods.size)

        assertContainsElements(
            features,
            "featureZ.fileutils",
            "FEATURE_ONE",
        )
        assertEquals(2, features.size)
    }

    fun testGetMethodsAndFeaturesInContainingFile_EmptyClass() = runBlocking {
        val element = loadElement("psi/util/EmptyClass.java", "/* EMPTY_CLASS_MARKER */")
        val (methods, features) = PsiFileUtils.getMethodsAndFeaturesInContainingFile(element)
        assertTrue(methods.isEmpty())
        assertTrue(features.isEmpty())
    }

    fun testExtractMappingPathsAndMethods_Controller() = runBlocking {
        val element = loadElement("psi/file/ControllerForFileUtils.java", "/* FILE_UTILS_METHOD_ONE_MARKER */")
        val (paths, httpMethods) = PsiFileUtils.extractMappingPathsAndMethods(element)

        assertContainsElements(
            paths,
            "/class-level-fileutils",
            "/class-level-fileutils/methodOneFileUtils",
            "/class-level-fileutils/methodTwoFileUtils",
            "/class-level-fileutils/methodThreeFileUtils"
        )
        assertEquals(4, paths.size)

        assertContainsElements(httpMethods, "GET", "POST", "PUT")
        assertEquals(3, httpMethods.size)
    }

    fun testExtractMappingPathsAndMethods_EmptyClass() = runBlocking {
        val element = loadElement("psi/util/EmptyClass.java", "/* EMPTY_CLASS_MARKER */")
        val (paths, httpMethods) = PsiFileUtils.extractMappingPathsAndMethods(element)

        assertTrue(paths.isEmpty())
        assertTrue(httpMethods.isEmpty())
    }

    fun testExtractMappingPathsAndMethods_NoSpring() = runBlocking {
        val psiFile = myFixture.configureByText(
            "NoSpringFileUtils.java",
            """
            package com.example.fileutils;
            public class NoSpringFileUtils { /* NO_SPRING_FILE_UTILS_MARKER */
                public void someMethod() {}
            }
            """.trimIndent()
        )
        val element = psiFile.getElementAtMarker("/* NO_SPRING_FILE_UTILS_MARKER */")
        val (paths, httpMethods) = PsiFileUtils.extractMappingPathsAndMethods(element)
        assertTrue(paths.isEmpty())
        assertTrue(httpMethods.isEmpty())
    }
}
