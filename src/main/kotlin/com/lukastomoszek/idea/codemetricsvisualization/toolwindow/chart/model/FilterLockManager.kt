package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.model

class FilterLockManager {
    var isMethodFilterLocked: Boolean = false
    var isFeatureFilterLocked: Boolean = false

    fun toggleMethodLock(): Boolean {
        isMethodFilterLocked = !isMethodFilterLocked
        return isMethodFilterLocked
    }

    fun toggleFeatureLock(): Boolean {
        isFeatureFilterLocked = !isFeatureFilterLocked
        return isFeatureFilterLocked
    }
}
