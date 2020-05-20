package security.fixes

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException
import com.jetbrains.python.psi.*

class JinjaAutoinspectUnconditionalFixer : LocalQuickFix, IntentionAction, HighPriorityAction {
    override fun getText(): String {
        return name
    }

    override fun getFamilyName(): String {
        return "Add autoinspect unconditionally"
    }

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        return true
    }

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        ApplicationManager.getApplication().runWriteAction {
            getPyCallExpressionAtCaret(file, editor)?.let { runFix(project, file, it) }
        }
    }

    override fun startInWriteAction(): Boolean {
        return true
    }

    fun runFix(project: Project, file: PsiFile, originalElement: PsiElement): PyCallExpression? {
        if (originalElement !is PyCallExpression) return null
        if (file !is PyFile) return null
        val newEl = originalElement.copy() as PyCallExpression
        val autoescapeArgument = newEl.getKeywordArgument("autoescape")
        val elementGenerator = PyElementGenerator.getInstance(project)
        val newArg = elementGenerator.createKeywordArgument(file.languageLevel, "autoescape", "True")
        if (autoescapeArgument == null) {
            addKeywordArgument(newEl, newArg)
        } else {
            if (autoescapeArgument is PyCallExpression) return null
            if (autoescapeArgument !is PyBoolLiteralExpression) return null
            autoescapeArgument.replace(elementGenerator.createFromText(file.languageLevel, PyExpressionStatement::class.java, "True"))
        }
        originalElement.replace(newEl)
        return newEl
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        runFix(project, descriptor.psiElement.containingFile, descriptor.psiElement)
    }
}