package com.lukastomoszek.idea.codemetricsvisualization.toolwindow.chart.listener

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.lukastomoszek.idea.codemetricsvisualization.context.PsiContextResolver
import com.lukastomoszek.idea.codemetricsvisualization.context.PsiUtils
import com.lukastomoszek.idea.codemetricsvisualization.context.model.ContextInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class ChartContextListener(
    private val project: Project,
    private val cs: CoroutineScope,
    private val onContextUpdated: (ContextInfo) -> Unit
) : Disposable {

    private var scheduledUpdate: ScheduledFuture<*>? = null
    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        val thread = Thread(r, "ChartContextUpdateScheduler")
        thread.isDaemon = true
        thread
    }

    private val caretListener = object : CaretListener {
        override fun caretPositionChanged(event: CaretEvent) = scheduleContextUpdate(event.editor, event.caret?.offset)
    }

    private val documentListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            editor?.let { scheduleContextUpdate(it, it.caretModel.offset) }
        }
    }

    private val editorFactoryListener = object : EditorFactoryListener {
        override fun editorCreated(event: EditorFactoryEvent) {
            if (event.editor.project == project) {
                event.editor.caretModel.addCaretListener(caretListener, this@ChartContextListener)
                event.editor.document.addDocumentListener(documentListener, this@ChartContextListener)
                scheduleContextUpdate(event.editor, event.editor.caretModel.offset)
            }
        }

        override fun editorReleased(event: EditorFactoryEvent) {
            if (event.editor.project == project) {
                event.editor.caretModel.removeCaretListener(caretListener)
                event.editor.document.removeDocumentListener(documentListener)
            }
        }
    }

    private val fileEditorManagerListener = object : FileEditorManagerListener {
        override fun selectionChanged(event: FileEditorManagerEvent) {
            val editor =
                event.newEditor?.let { if (it is com.intellij.openapi.fileEditor.TextEditor) it.editor else null }
            scheduleContextUpdate(editor, editor?.caretModel?.offset)
        }
    }

    fun register() {
        EditorFactory.getInstance().addEditorFactoryListener(editorFactoryListener, this)
        EditorFactory.getInstance().allEditors.forEach { editor ->
            if (editor.project == project) {
                editor.caretModel.addCaretListener(caretListener, this)
                editor.document.addDocumentListener(documentListener, this)
            }
        }
        project.messageBus.connect(this)
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerListener)

        FileEditorManager.getInstance(project).selectedTextEditor?.let {
            scheduleContextUpdate(it, it.caretModel.offset)
        }
    }

    private fun scheduleContextUpdate(editor: Editor?, offset: Int?) {
        val currentEditor = editor ?: FileEditorManager.getInstance(project).selectedTextEditor ?: return

        if (project.isDisposed) return

        val currentOffset = offset ?: currentEditor.caretModel.offset
        val file = FileDocumentManager.getInstance().getFile(currentEditor.document) ?: return
        if (!file.isValid) return

        scheduledUpdate?.cancel(false)
        scheduledUpdate = scheduler.schedule({
            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed || currentEditor.isDisposed) return@invokeLater
                cs.launch {
                    val contextInfo =
                        PsiUtils.findPsiElementAtOffset(project, currentEditor, currentOffset)?.let {
                            PsiContextResolver.getContextInfoFromPsi(it)
                        }
                    onContextUpdated(contextInfo ?: ContextInfo(null, null, null, null))
                }
            }
        }, 300, TimeUnit.MILLISECONDS)
    }

    override fun dispose() {
        EditorFactory.getInstance().allEditors.forEach { editor ->
            if (editor.project == project) {
                editor.caretModel.removeCaretListener(caretListener)
                editor.document.removeDocumentListener(documentListener)
            }
        }
        scheduler.shutdownNow()
    }
}
