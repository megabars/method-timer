package com.github.methodtimer.plugin.codevision

import com.github.methodtimer.plugin.services.MethodTimingStorage
import com.github.methodtimer.plugin.util.MethodSignatureResolver
import com.github.methodtimer.plugin.util.TimeFormatter
import com.intellij.codeInsight.codeVision.*
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import java.awt.event.MouseEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MethodTimingCodeVisionProvider : CodeVisionProvider<Unit> {

    override val id: String = ID
    override val name: String = "Method Execution Time"
    override val defaultAnchor: CodeVisionAnchorKind = CodeVisionAnchorKind.Top
    override val relativeOrderings: List<CodeVisionRelativeOrdering> =
        listOf(CodeVisionRelativeOrdering.CodeVisionRelativeOrderingAfter("java.inheritors"))
    override val groupId: String = id

    override fun precomputeOnUiThread(editor: Editor) {}

    override fun computeCodeVision(editor: Editor, uiData: Unit): CodeVisionState {
        val project = editor.project ?: return CodeVisionState.READY_EMPTY

        val storage = MethodTimingStorage.getInstance(project)
        val lastRunTimestamp = storage.state.lastRunTimestamp

        val lenses = ReadAction.compute<List<Pair<TextRange, CodeVisionEntry>>, Throwable> {
            val psiFile = com.intellij.psi.PsiDocumentManager.getInstance(project)
                .getPsiFile(editor.document) as? PsiJavaFile ?: return@compute emptyList()

            val result = mutableListOf<Pair<TextRange, CodeVisionEntry>>()
            PsiTreeUtil.findChildrenOfType(psiFile, PsiMethod::class.java).forEach { method ->
                val fqn = MethodSignatureResolver.resolveSignature(method) ?: return@forEach
                val nanos = storage.getTimingForMethod(fqn) ?: return@forEach

                val textRange = method.nameIdentifier?.textRange ?: return@forEach
                val formattedTime = TimeFormatter.format(nanos)

                val entry = ClickableTextCodeVisionEntry(
                    "\u23F1 $formattedTime",
                    id,
                    { event, sourceEditor -> handleClick(event, sourceEditor, fqn, nanos, lastRunTimestamp) },
                    null,
                    formattedTime,
                    ""
                )
                result.add(textRange to entry)
            }
            result
        }

        return CodeVisionState.Ready(lenses)
    }

    private fun handleClick(
        @Suppress("UNUSED_PARAMETER") event: MouseEvent?,
        editor: Editor,
        fqn: String,
        nanos: Long,
        lastRunTimestamp: Long
    ) {
        val formattedTime = TimeFormatter.format(nanos)
        val dateStr = if (lastRunTimestamp > 0) {
            DATE_FORMATTER.format(Instant.ofEpochMilli(lastRunTimestamp).atZone(ZoneId.systemDefault()))
        } else {
            "unknown"
        }

        val message = """
            |Method: $fqn
            |Execution time: $formattedTime
            |Last run: $dateStr
        """.trimMargin()

        JBPopupFactory.getInstance()
            .createMessage(message)
            .showInBestPositionFor(editor)
    }

    companion object {
        const val ID = "method.timing"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
