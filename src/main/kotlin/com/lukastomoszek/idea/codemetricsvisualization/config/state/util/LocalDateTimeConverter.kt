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
