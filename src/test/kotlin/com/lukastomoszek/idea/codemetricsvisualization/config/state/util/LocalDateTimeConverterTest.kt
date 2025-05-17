package com.lukastomoszek.idea.codemetricsvisualization.config.state.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class LocalDateTimeConverterTest {

    private val converter = LocalDateTimeConverter()

    @Test
    fun testFromStringConvertsEpochMillisToLocalDateTime() {
        val testDateTime = LocalDateTime.of(2023, 10, 26, 10, 30, 0)
        val epochMilli = testDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val epochMilliString = epochMilli.toString()

        val result = converter.fromString(epochMilliString)
        assertEquals(testDateTime, result)
    }

    @Test(expected = NumberFormatException::class)
    fun testFromStringThrowsOnInvalidInput() {
        converter.fromString("not_a_long")
    }

    @Test
    fun testToStringConvertsLocalDateTimeToEpochMillis() {
        val testDateTime = LocalDateTime.of(2023, 10, 26, 12, 45, 15)
        val expectedEpochMilli = testDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val expectedEpochMilliString = expectedEpochMilli.toString()

        val result = converter.toString(testDateTime)
        assertEquals(expectedEpochMilliString, result)
    }

    @Test
    fun testToStringReturnsNullStringForNullInput() {
        val result = converter.toString(null)
        assertEquals("null", result)
    }

    @Test
    fun testFromStringAndToStringAreSymmetric() {
        val originalDateTime = LocalDateTime.now()
        val stringRepresentation = converter.toString(originalDateTime)
        val restoredDateTime = converter.fromString(stringRepresentation!!)

        assertEquals(originalDateTime.withNano(0), restoredDateTime.withNano(0))
    }
}
