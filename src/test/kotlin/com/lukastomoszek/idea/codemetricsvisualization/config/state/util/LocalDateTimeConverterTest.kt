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

package com.lukastomoszek.idea.codemetricsvisualization.config.state.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun testFromStringReturnsNullOnInvalidInput() {
        assertNull(converter.fromString("not_a_long"))
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
    fun testFromStringReturnsNullForLiteralNullString() {
        assertNull(converter.fromString("null"))
    }

    @Test
    fun testFromStringAndToStringAreSymmetric() {
        val originalDateTime = LocalDateTime.now()
        val stringRepresentation = converter.toString(originalDateTime)
        val restoredDateTime = converter.fromString(stringRepresentation!!)

        assertEquals(originalDateTime.withNano(0), restoredDateTime?.withNano(0))
    }
}
