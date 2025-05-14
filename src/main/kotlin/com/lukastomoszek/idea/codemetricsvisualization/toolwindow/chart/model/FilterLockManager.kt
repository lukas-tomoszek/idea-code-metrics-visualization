package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.model

class FilterLockManager {
    var isMethodFilterLocked: Boolean = false
        private set
    var isFeatureFilterLocked: Boolean = false
        private set

    fun toggleMethodLock(): Boolean {
        isMethodFilterLocked = !isMethodFilterLocked
        return isMethodFilterLocked
    }

    fun toggleFeatureLock(): Boolean {
        isFeatureFilterLocked = !isFeatureFilterLocked
        return isFeatureFilterLocked
    }

    fun setMethodLock(locked: Boolean) {
        isMethodFilterLocked = locked
    }

    fun setFeatureLock(locked: Boolean) {
        isFeatureFilterLocked = locked
    }
}
