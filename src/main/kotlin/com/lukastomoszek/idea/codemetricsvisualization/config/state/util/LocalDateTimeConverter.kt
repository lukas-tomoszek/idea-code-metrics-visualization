package com.lukastomoszek.idea.codemetricsvisualization.config.state.util

import com.intellij.util.xmlb.Converter
import org.jetbrains.annotations.NotNull

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

internal class LocalDateTimeConverter : Converter<LocalDateTime?>() {
    override fun fromString(@NotNull value: String): LocalDateTime {
        val epochMilli = value.toLong()
        val zoneId = ZoneId.systemDefault()
        return Instant.ofEpochMilli(epochMilli)
            .atZone(zoneId)
            .toLocalDateTime()
    }

    override fun toString(value: LocalDateTime?): String? {
        val zoneId = ZoneId.systemDefault()
        val toEpochMilli: Long? = value?.atZone(zoneId)
            ?.toInstant()
            ?.toEpochMilli()
        return toEpochMilli.toString()
    }
}
