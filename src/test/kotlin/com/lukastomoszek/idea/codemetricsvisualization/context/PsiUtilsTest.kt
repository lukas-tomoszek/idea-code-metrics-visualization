package com.lukastomoszek.idea.codemetricsvisualization.context

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking

private const val TEST_FILE_PATH = "psi/PsiUtilsTestData.java"

class PsiUtilsTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String {
        return "src/test/resources/testdata"
    }

    private fun getElementAtMarker(psiFile: PsiFile, marker: String): PsiElement {
        val text = psiFile.text
        val offset = text.indexOf(marker)
        assertTrue("Marker '$marker' not found in file ${psiFile.name}", offset != -1)
        val elementAtMarker = psiFile.findElementAt(offset)
        assertNotNull("Element at marker '$marker' should not be null", elementAtMarker)
        return elementAtMarker!!
    }

    fun testGetContainingMethodFqnForSimpleMethod() = runBlocking {
        val element =
            getElementAtMarker(myFixture.configureByFile(TEST_FILE_PATH), "// SIMPLE_METHOD_MARKER")
        assertEquals("com.example.TopLevelClass.simpleMethod", PsiUtils.getContainingMethodFqn(element))
    }

    fun testGetContainingMethodFqnForMethodInNestedClass() = runBlocking {
        val element =
            getElementAtMarker(myFixture.configureByFile(TEST_FILE_PATH), "// NESTED_CLASS_METHOD_MARKER")
        assertEquals(
            "com.example.TopLevelClass.NestedClass.methodInNestedClass",
            PsiUtils.getContainingMethodFqn(element)
        )
    }

    fun testGetContainingMethodFqnForLambdaExpression() = runBlocking {
        val element = getElementAtMarker(myFixture.configureByFile(TEST_FILE_PATH), "// LAMBDA_MARKER")
        assertEquals("com.example.TopLevelClass.methodWithLambda", PsiUtils.getContainingMethodFqn(element))
    }

    fun testGetContainingMethodFqnForStaticMethod() = runBlocking {
        val element =
            getElementAtMarker(myFixture.configureByFile(TEST_FILE_PATH), "// STATIC_METHOD_MARKER")
        assertEquals("com.example.TopLevelClass.staticMethod", PsiUtils.getContainingMethodFqn(element))
    }

    fun testGetContainingMethodFqnForConstructor() = runBlocking {
        val element =
            getElementAtMarker(myFixture.configureByFile(TEST_FILE_PATH), "// CONSTRUCTOR_MARKER")
        assertEquals("com.example.TopLevelClass.TopLevelClass", PsiUtils.getContainingMethodFqn(element))
    }

    fun testGetContainingMethodFqnForMethodInClassWithoutPackage() = runBlocking {
        val element = getElementAtMarker(
            myFixture.configureByFile(TEST_FILE_PATH),
            "// NO_PACKAGE_CLASS_METHOD_MARKER"
        )
        assertEquals(
            "com.example.ClassWithoutPackage.methodInClassWithoutPackage",
            PsiUtils.getContainingMethodFqn(element)
        )
    }

    fun testGetContainingMethodFqnForElementAtClassFieldLevel() = runBlocking {
        val element = getElementAtMarker(myFixture.configureByFile(TEST_FILE_PATH), "// FIELD_MARKER")
        assertNull(PsiUtils.getContainingMethodFqn(element))
    }

    fun testGetContainingMethodFqnForElementAtTopLevelComment() = runBlocking {
        val element =
            getElementAtMarker(myFixture.configureByFile(TEST_FILE_PATH), "// TOP_LEVEL_COMMENT_MARKER")
        assertNull(PsiUtils.getContainingMethodFqn(element))
    }

    fun testGetPsiFile() = runBlocking {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val retrieved = PsiUtils.getPsiFile(myFixture.editor, myFixture.project)
        assertNotNull(retrieved)
        assertEquals(psiFile.virtualFile.path, retrieved?.virtualFile?.path)
    }

    fun testFindPsiElementAtOffset() = runBlocking {
        val psiFile = myFixture.configureByFile(TEST_FILE_PATH)
        val offset = psiFile.text.indexOf("// SIMPLE_METHOD_MARKER")
        assertTrue(offset != -1)

        val element = PsiUtils.findPsiElementAtOffset(psiFile, offset)
        assertNotNull(element)
        assertTrue(element!!.text.startsWith("// SIMPLE_METHOD_MARKER"))
    }

    fun testGetContainingMethodFqnReturnsNullIfNoClassOrMethod() = runBlocking {
        val psiFile = myFixture.configureByText("Dummy.java", "int x = 0;")
        val element = psiFile.findElementAt(0)!!
        assertNull(PsiUtils.getContainingMethodFqn(element))
    }

    fun testGetContainingMethodFqnInAnonymousClass() = runBlocking {
        val psiFile = myFixture.configureByText(
            "AnonClass.java", """
            package com.example;
            public class AnonClass {
                void test() {
                    Runnable r = new Runnable() {
                        public void run() {
                            int x = 1; // ANON_MARKER
                        }
                    };
                }
            }
        """.trimIndent()
        )
        val element = getElementAtMarker(psiFile, "// ANON_MARKER")
        assertEquals("run", PsiUtils.getContainingMethodFqn(element))
    }

    fun testGetContainingMethodFqnStaticNestedClass() = runBlocking {
        val psiFile = myFixture.configureByText(
            "StaticNested.java", """
            package com.example;
            public class Outer {
                static class StaticNested {
                    void inner() { int x = 0; // STATIC_NESTED_MARKER }
                }
            }
        """.trimIndent()
        )
        val element = getElementAtMarker(psiFile, "// STATIC_NESTED_MARKER")
        assertEquals("com.example.Outer.StaticNested.inner", PsiUtils.getContainingMethodFqn(element))
    }

    fun testFindPsiElementAtOffsetOutOfBounds() = runBlocking {
        val psiFile = myFixture.configureByText("Dummy.java", "class A {}")
        val element = PsiUtils.findPsiElementAtOffset(psiFile, 999)
        assertNull(element)
    }
}
