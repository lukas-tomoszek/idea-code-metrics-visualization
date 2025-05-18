package com.lukastomoszek.idea.codemetricsvisualization.linemarker.rule

import org.junit.Assert.*
import org.junit.Test
import java.awt.Color

class RuleEvaluatorTest {

    @Test
    fun parseBoundaryStringEmptyStringForFromBoundaryReturnsNegativeInfinity() {
        assertEquals(Float.NEGATIVE_INFINITY, RuleEvaluator.parseBoundaryString("", true))
    }

    @Test
    fun parseBoundaryStringEmptyStringForToBoundaryReturnsPositiveInfinity() {
        assertEquals(Float.POSITIVE_INFINITY, RuleEvaluator.parseBoundaryString("", false))
    }

    @Test
    fun parseBoundaryStringValidPositiveFloatString() {
        assertEquals(10.5f, RuleEvaluator.parseBoundaryString("10.5", true))
    }

    @Test
    fun parseBoundaryStringValidNegativeFloatString() {
        assertEquals(-5.0f, RuleEvaluator.parseBoundaryString("-5.0", false))
    }

    @Test
    fun parseBoundaryStringZeroString() {
        assertEquals(0.0f, RuleEvaluator.parseBoundaryString("0", true))
    }

    @Test
    fun parseBoundaryStringStringWithWhitespace() {
        assertEquals(20.0f, RuleEvaluator.parseBoundaryString("  20  ", false))
    }

    @Test(expected = NumberFormatException::class)
    fun parseBoundaryStringInvalidNumberStringThrowsNumberFormatException() {
        RuleEvaluator.parseBoundaryString("abc", true)
    }

    @Test
    fun evaluateNullValueReturnsNullColor() {
        val rules = listOf(RangeRule("0", "10", "#FF0000"))
        assertNull(RuleEvaluator.evaluate(null, rules))
    }

    @Test
    fun evaluateNoRulesReturnsNullColor() {
        assertNull(RuleEvaluator.evaluate(5f, emptyList()))
    }

    @Test
    fun evaluateValueMatchesRuleWithValidColor() {
        val rules = listOf(RangeRule("0", "10", "#FF0000"))
        assertEquals(Color.RED, RuleEvaluator.evaluate(5f, rules))
        assertEquals(Color.RED, RuleEvaluator.evaluate(10f, rules))
    }

    @Test
    fun evaluateValueDoesNotMatchExclusiveFrom() {
        val rules = listOf(RangeRule("0", "10", "#FF0000"))
        assertNull(RuleEvaluator.evaluate(0f, rules))
    }

    @Test
    fun evaluateValueMatchesRuleWithEmptyColorHexReturnsNull() {
        val rules = listOf(RangeRule("0", "10", ""))
        assertNull(RuleEvaluator.evaluate(5f, rules))
    }

    @Test
    fun evaluateValueMatchesRuleWithInvalidColorHexReturnsNull() {
        val rules = listOf(RangeRule("0", "10", "invalidColor"))
        assertNull(RuleEvaluator.evaluate(5f, rules))
    }

    @Test
    fun evaluateValueFallsBetweenRulesReturnsNull() {
        val rules = listOf(
            RangeRule("", "0", "#00FF00"),
            RangeRule("10", "20", "#0000FF")
        )
        assertNull(RuleEvaluator.evaluate(5f, rules))
    }

    @Test
    fun evaluateValueMatchesFirstApplicableRule() {
        val rules = listOf(
            RangeRule("0", "10", "#FF0000"),
            RangeRule("5", "15", "#00FF00")
        )
        assertEquals(Color.RED, RuleEvaluator.evaluate(7f, rules))
    }

    @Test
    fun evaluateValueMatchesRuleWithOpenFromStringBoundary() {
        val rules = listOf(RangeRule("", "0", "#00FF00"))
        assertEquals(Color.GREEN, RuleEvaluator.evaluate(-5f, rules))
        assertEquals(Color.GREEN, RuleEvaluator.evaluate(0f, rules))
    }

    @Test
    fun evaluateValueMatchesRuleWithOpenToStringBoundary() {
        val rules = listOf(RangeRule("100", "", "#FF00FF"))
        assertEquals(Color.MAGENTA, RuleEvaluator.evaluate(101f, rules))
        assertEquals(Color.MAGENTA, RuleEvaluator.evaluate(1000f, rules))
    }

    @Test
    fun evaluateComplexRuleSet() {
        val rules = listOf(
            RangeRule("50", "", "#FF0000"),
            RangeRule("20", "50", "#FFFF00"),
            RangeRule("0", "20", "#00FF00"),
            RangeRule("", "0", "#0000FF")
        )
        assertEquals(Color.RED, RuleEvaluator.evaluate(75f, rules))
        assertEquals(Color.YELLOW, RuleEvaluator.evaluate(50f, rules))
        assertEquals(Color.YELLOW, RuleEvaluator.evaluate(20.1f, rules))
        assertEquals(Color.GREEN, RuleEvaluator.evaluate(20f, rules))
        assertEquals(Color.GREEN, RuleEvaluator.evaluate(0.1f, rules))
        assertEquals(Color.BLUE, RuleEvaluator.evaluate(0f, rules))
        assertEquals(Color.BLUE, RuleEvaluator.evaluate(-10f, rules))
    }

    @Test
    fun evaluateRuleWithInvalidBoundaryStringThrows() {
        val rules = listOf(
            RangeRule("abc", "10", "#FF0000"),
            RangeRule("10", "20", "#00FF00")
        )

        assertThrows(AssertionError::class.java) {
            RuleEvaluator.evaluate(15f, rules)
        }
    }

}
