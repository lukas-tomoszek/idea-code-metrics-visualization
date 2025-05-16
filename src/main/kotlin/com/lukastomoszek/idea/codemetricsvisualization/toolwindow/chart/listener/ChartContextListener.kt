package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.listener

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.lukastomoszek.idea.codemetricsvisualization.context.PsiContextResolver
import com.lukastomoszek.idea.codemetricsvisualization.context.PsiUtils
import com.lukastomoszek.idea.codemetricsvisualization.context.model.ContextInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChartContextListener(
    private val project: Project,
    private val cs: CoroutineScope,
    private val onContextUpdated: (ContextInfo) -> Unit
) : Disposable {

    private var updateJob: Job? = null

    private val caretListener = object : CaretListener {
        override fun caretPositionChanged(event: CaretEvent) = scheduleContextUpdate()
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
        override fun selectionChanged(event: FileEditorManagerEvent) {
            scheduleContextUpdate()
        }
    }

    fun register() {
        EditorFactory.getInstance().addEditorFactoryListener(editorFactoryListener, this)
        EditorFactory.getInstance().allEditors
            .filter { it.project == project }
            .forEach { it.caretModel.addCaretListener(caretListener, this) }

        project.messageBus.connect(this)
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerListener)
    }

    private fun scheduleContextUpdate() {
        updateJob?.cancel()
        updateJob = cs.launch {
            delay(300)
            if (project.isDisposed) return@launch

            val editor = readAction {
                FileEditorManager.getInstance(project).selectedTextEditor
            } ?: return@launch

            if (editor.isDisposed) return@launch
            val offset = readAction { editor.caretModel.offset }

            val psiElement = PsiUtils.findPsiElementAtOffset(project, editor, offset)
            val contextInfo = psiElement?.let { PsiContextResolver.getContextInfoFromPsi(it) } ?: return@launch

            onContextUpdated(contextInfo)
        }
    }

    override fun dispose() {
        EditorFactory.getInstance().allEditors
            .filter { it.project == project }
            .forEach { it.caretModel.removeCaretListener(caretListener) }
    }
}
