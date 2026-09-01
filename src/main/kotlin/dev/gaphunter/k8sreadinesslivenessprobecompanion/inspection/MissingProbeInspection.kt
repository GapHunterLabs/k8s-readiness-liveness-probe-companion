package dev.gaphunter.k8sreadinesslivenessprobecompanion.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import dev.gaphunter.k8sreadinesslivenessprobecompanion.detect.K8sProbeManifestScanner
import dev.gaphunter.k8sreadinesslivenessprobecompanion.model.ProbeHit
import dev.gaphunter.k8sreadinesslivenessprobecompanion.model.ProbeProblem
import dev.gaphunter.k8sreadinesslivenessprobecompanion.review.ReviewPrompt

/**
 * Flags a Kubernetes workload manifest's container entry that has no
 * `readinessProbe:` and/or no `livenessProbe:` -- without a
 * `readinessProbe`, Kubernetes can't take a sick pod out of the load
 * balancer; without a `livenessProbe`, a hung (not crashed) process is
 * never automatically restarted.
 *
 * Runs via [checkFile] (whole-file text scan), same reasoning as this
 * catalog's own `k8s-resource-limit-companion`: detection is
 * plain-text line scanning against indentation, not a PSI walk of a
 * specific grammar -- see `build.gradle.kts` for why no YAML PSI
 * dependency is taken.
 */
class MissingProbeInspection : LocalInspectionTool() {

    companion object {
        const val MAX_FILE_LENGTH = 500_000
        private val YAML_FILE_NAME = Regex("""^[^.]+\.ya?ml$""", RegexOption.IGNORE_CASE)
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        val virtualFile = file.virtualFile ?: return null
        if (!YAML_FILE_NAME.matches(virtualFile.name)) return null

        val text = file.text
        if (text.length > MAX_FILE_LENGTH) return null

        val hits = K8sProbeManifestScanner.scan(text)
        if (hits.isEmpty()) return null

        val document = file.viewProvider.document ?: return null
        val problems = mutableListOf<ProblemDescriptor>()

        for (hit in hits) {
            if (hit.lineNumber - 1 !in 0 until document.lineCount) continue
            val lineStartOffset = document.getLineStartOffset(hit.lineNumber - 1)
            val lineEndOffset = document.getLineEndOffset(hit.lineNumber - 1)
            val anchor = leafElementAt(file, lineStartOffset) ?: continue
            val anchorStart = anchor.textRange.startOffset
            val relativeRange = TextRange(
                (lineStartOffset - anchorStart).coerceAtLeast(0),
                (lineEndOffset - anchorStart).coerceAtMost(anchor.textLength),
            )
            if (relativeRange.startOffset >= relativeRange.endOffset) continue

            problems += manager.createProblemDescriptor(
                anchor,
                relativeRange,
                messageFor(hit),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                isOnTheFly,
            )

            ReviewPrompt.recordHit(file.project, "${virtualFile.path}:${hit.lineNumber}:${hit.problem}")
        }

        return if (problems.isEmpty()) null else problems.toTypedArray()
    }

    private fun messageFor(hit: ProbeHit): String = when (hit.problem) {
        ProbeProblem.MISSING_READINESS_PROBE -> "Container '${hit.containerLabel}' has no readinessProbe -- Kubernetes can't take it out of the load balancer if it's not actually ready, so traffic keeps being routed to a broken instance"
        ProbeProblem.MISSING_LIVENESS_PROBE -> "Container '${hit.containerLabel}' has no livenessProbe -- a hung (not crashed) process is never automatically restarted"
    }

    private fun leafElementAt(file: PsiFile, startOffset: Int): PsiElement? {
        if (startOffset < 0 || startOffset >= file.textLength) return null
        var element = file.findElementAt(startOffset) ?: return file
        while (element.firstChild != null) {
            element = element.firstChild
        }
        return element
    }
}
