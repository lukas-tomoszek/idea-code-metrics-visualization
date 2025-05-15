package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.ui

import com.intellij.ui.JBColor
import org.knowm.xchart.CategoryChart
import org.knowm.xchart.style.AxesChartStyler
import org.knowm.xchart.style.Styler
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

object ChartXChartConfigurator {

    fun configureChartStyles(
        chart: CategoryChart,
        labels: List<Any>,
        values: List<List<Number>>
    ) = with(chart.styler) {
        chartBackgroundColor = JBColor.PanelBackground
        plotBackgroundColor = JBColor.PanelBackground
        plotBorderColor = JBColor.border()
        axisTickLabelsColor = JBColor.foreground()

        yAxisLabelAlignment = AxesChartStyler.TextAlignment.Right
        isXAxisTitleVisible = false
        isYAxisTitleVisible = false

        isPlotGridLinesVisible = false
        isLegendVisible = false

        isToolTipsEnabled = true
        isToolTipsAlwaysVisible = false
        toolTipHighlightColor = JBColor.ORANGE

        val showXLabels = labels.size <= 100
        val verticalXLabels = labels.size > 20
        isXAxisTicksVisible = showXLabels
        xAxisLabelRotation = if (verticalXLabels) 90 else 0
        toolTipType = if (showXLabels) Styler.ToolTipType.yLabels else Styler.ToolTipType.xAndYLabels

        val allValuesAreIntegers = values.flatten().all { (it.toDouble() % 1.0) == 0.0 }
        decimalPattern = if (allValuesAreIntegers) "#,###" else "#,##0.00"

        this.datePattern = determineDatePattern(labels)
    }

    fun determineDatePattern(dates: List<Any?>): String? {
        val javaDates = dates.filterIsInstance<Date>()
        if (javaDates.isEmpty()) return null

        val zoned = javaDates.map { it.toInstant().atZone(ZoneId.systemDefault()) }

        val allDateOnly = zoned.all { it.toLocalTime() == LocalTime.MIDNIGHT }
        val allTimeOnly = zoned.all { it.toLocalDate() == LocalDate.of(1970, 1, 1) }

        return when {
            allTimeOnly -> "HH:mm:ss"
            allDateOnly -> "yyyy-MM-dd"
            else -> "MM-dd HH:mm"
        }
    }
}
