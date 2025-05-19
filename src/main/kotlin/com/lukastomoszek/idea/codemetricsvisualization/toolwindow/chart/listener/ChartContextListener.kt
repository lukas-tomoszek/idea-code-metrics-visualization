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

package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.listener

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project

class ChartContextListener(
    private val project: Project,
    private val onContextUpdated: () -> Unit
) : Disposable {

    fun register() {
        EditorFactory.getInstance().addEditorFactoryListener(editorFactoryListener, this)
        EditorFactory.getInstance().allEditors
            .filter { it.project == project }
            .forEach { it.caretModel.addCaretListener(caretListener, this) }

        project.messageBus.connect(this)
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerListener)
    }

    private val caretListener = object : CaretListener {
        override fun caretPositionChanged(event: CaretEvent) = onContextUpdated()
    }

    private val editorFactoryListener = object : EditorFactoryListener {
        override fun editorCreated(event: EditorFactoryEvent) {
            if (event.editor.project == project) {
                event.editor.caretModel.addCaretListener(caretListener, this@ChartContextListener)
            }
        }

        override fun editorReleased(event: EditorFactoryEvent) {
            if (event.editor.project == project) {
                event.editor.caretModel.removeCaretListener(caretListener)
            }
        }
    }

    private val fileEditorManagerListener = object : FileEditorManagerListener {
        override fun selectionChanged(event: FileEditorManagerEvent) = onContextUpdated()
    }

    override fun dispose() {
        EditorFactory.getInstance().allEditors
            .filter { it.project == project }
            .forEach { it.caretModel.removeCaretListener(caretListener) }
    }
}
