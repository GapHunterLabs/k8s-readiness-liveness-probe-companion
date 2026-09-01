package dev.gaphunter.k8sreadinesslivenessprobecompanion.model

enum class ProbeProblem {
    /** No `readinessProbe:` key anywhere in this container's own body. */
    MISSING_READINESS_PROBE,

    /** No `livenessProbe:` key anywhere in this container's own body. */
    MISSING_LIVENESS_PROBE,
}

/** One container entry (by name) missing a readiness and/or liveness probe. */
data class ProbeHit(
    val containerLabel: String,
    val problem: ProbeProblem,
    val lineNumber: Int,
)
