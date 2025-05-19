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

import com.intellij.util.xmlb.Converter
import org.jetbrains.annotations.NotNull
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

internal class LocalDateTimeConverter : Converter<LocalDateTime?>() {
    override fun fromString(@NotNull value: String): LocalDateTime? {
        return try {
            val epochMilli = value.toLong()
            val zoneId = ZoneId.systemDefault()
            Instant.ofEpochMilli(epochMilli)
                .atZone(zoneId)
                .toLocalDateTime()
        } catch (e: NumberFormatException) {
            null
        }
    }

    override fun toString(value: LocalDateTime?): String? {
        if (value == null) {
            return null
        }
        val zoneId = ZoneId.systemDefault()
        return value.atZone(zoneId)
            .toInstant()
            .toEpochMilli()
            .toString()
    }
}
