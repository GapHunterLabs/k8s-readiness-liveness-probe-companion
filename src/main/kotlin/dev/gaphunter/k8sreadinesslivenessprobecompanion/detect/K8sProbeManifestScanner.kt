package dev.gaphunter.k8sreadinesslivenessprobecompanion.detect

import dev.gaphunter.k8sreadinesslivenessprobecompanion.model.ProbeHit
import dev.gaphunter.k8sreadinesslivenessprobecompanion.model.ProbeProblem

/**
 * Plain-text line scanner for Kubernetes workload manifests (YAML) --
 * flags any container entry under a `containers:` (or
 * `initContainers:`) list that has no `readinessProbe:` and/or no
 * `livenessProbe:` key anywhere in its own body. Without a
 * `readinessProbe`, Kubernetes can't take a sick pod out of the load
 * balancer -- real traffic keeps being routed to a broken instance;
 * without a `livenessProbe`, a hung (not crashed) process is never
 * automatically restarted. Both are a real, recurring cause of
 * documented availability incidents.
 *
 * **Same scanner shape as this catalog's own
 * `k8s-resource-limit-companion`** (`K8sManifestScanner`) --
 * deliberately indentation-based, not a real YAML parser: a
 * container's own indentation level defines where its block ends (the
 * first line at or below that indentation that isn't blank/a
 * comment). Handles the common `containers:` / `- name: x` shape;
 * multi-doc files (`---` separators) and anchors/aliases aren't
 * specially resolved.
 *
 * **v0.1 scope, stated honestly:** only scans files whose `kind:` is a
 * workload kind that actually runs long-lived containers (Deployment,
 * Pod, StatefulSet, DaemonSet) -- `Job`/`CronJob` are deliberately
 * excluded here (unlike the sibling resource-limit scanner): a
 * run-to-completion Job/CronJob pod is not kept in a Service's load
 * balancer and Kubernetes doesn't restart it via liveness probing the
 * same way, so "missing probes" isn't the same real gap there. Never
 * validates that `initialDelaySeconds`/`periodSeconds` values are
 * reasonable, only the total absence of the probe block.
 */
object K8sProbeManifestScanner {

    private val WORKLOAD_KIND = Regex(
        """^kind:\s*["']?(Deployment|Pod|StatefulSet|DaemonSet)["']?\s*$""",
        RegexOption.IGNORE_CASE,
    )
    private val CONTAINERS_KEY = Regex("""^(\s*)(containers|initContainers):\s*$""")
    private val CONTAINER_NAME_ENTRY = Regex("""^(\s*)-\s*name:\s*["']?([\w.-]+)["']?\s*$""")
    private val READINESS_PROBE_KEY = Regex("""^\s*readinessProbe:\s*$""")
    private val LIVENESS_PROBE_KEY = Regex("""^\s*livenessProbe:\s*$""")

    fun scan(text: String): List<ProbeHit> {
        if (!looksLikeWorkloadManifest(text)) return emptyList()

        val lines = text.lines()
        val hits = mutableListOf<ProbeHit>()

        var i = 0
        while (i < lines.size) {
            val containersMatch = CONTAINERS_KEY.find(lines[i])
            if (containersMatch == null) {
                i++
                continue
            }
            val listIndent = containersMatch.groupValues[1].length
            i++

            // Walk each `- name: x` entry directly under this containers: list.
            while (i < lines.size) {
                val line = lines[i]
                if (isBlockEnd(line, listIndent)) break

                val entryMatch = CONTAINER_NAME_ENTRY.find(line)
                if (entryMatch == null) {
                    i++
                    continue
                }
                val entryIndent = entryMatch.groupValues[1].length
                val name = entryMatch.groupValues[2]
                val bodyStart = i + 1
                var bodyEnd = bodyStart
                while (bodyEnd < lines.size && !isBlockEnd(lines[bodyEnd], entryIndent)) bodyEnd++
                val body = lines.subList(bodyStart, bodyEnd)

                hits += problemsFor(name, body, i + 1)
                i = bodyEnd
            }
        }

        return hits
    }

    private fun problemsFor(name: String, body: List<String>, containerLineNumber: Int): List<ProbeHit> {
        val hits = mutableListOf<ProbeHit>()
        if (body.none { READINESS_PROBE_KEY.matches(it) }) {
            hits += ProbeHit(name, ProbeProblem.MISSING_READINESS_PROBE, containerLineNumber)
        }
        if (body.none { LIVENESS_PROBE_KEY.matches(it) }) {
            hits += ProbeHit(name, ProbeProblem.MISSING_LIVENESS_PROBE, containerLineNumber)
        }
        return hits
    }

    /** True when [line] is blank/comment (doesn't end a block), or is a real line at/below [indent] (ends it). */
    private fun isBlockEnd(line: String, indent: Int): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return false
        val lineIndent = line.length - line.trimStart().length
        return lineIndent <= indent
    }

    private fun looksLikeWorkloadManifest(text: String): Boolean =
        text.lineSequence().any { WORKLOAD_KIND.matches(it) }
}
