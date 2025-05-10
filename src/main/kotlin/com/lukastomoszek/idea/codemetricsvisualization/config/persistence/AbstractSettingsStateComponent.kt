package com.lukastomoszek.idea.codemetricsvisualization.config.persistence

import com.intellij.openapi.components.PersistentStateComponent

abstract class AbstractSettingsStateComponent<StateType : Any> : PersistentStateComponent<StateType> {

    protected abstract var internalState: StateType

    override fun getState(): StateType {
        return internalState
    }

    override fun loadState(state: StateType) {
        internalState = state
    }
}
