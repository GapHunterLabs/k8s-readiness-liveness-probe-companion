package dev.gaphunter.k8sreadinesslivenessprobecompanion.detect

import dev.gaphunter.k8sreadinesslivenessprobecompanion.model.ProbeProblem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class K8sProbeManifestScannerTest {

    @Test
    fun `container with no probes at all is flagged twice`() {
        val text = """
            kind: Deployment
            spec:
              template:
                spec:
                  containers:
                    - name: web
                      image: acmecorp/web:1.0
        """.trimIndent()
        val hits = K8sProbeManifestScanner.scan(text)
        assertEquals(2, hits.size)
        assertEquals(setOf(ProbeProblem.MISSING_READINESS_PROBE, ProbeProblem.MISSING_LIVENESS_PROBE), hits.map { it.problem }.toSet())
    }

    @Test
    fun `container with both probes is not flagged`() {
        val text = """
            kind: Deployment
            spec:
              template:
                spec:
                  containers:
                    - name: web
                      image: acmecorp/web:1.0
                      readinessProbe:
                        httpGet:
                          path: /healthz/ready
                          port: 8080
                      livenessProbe:
                        httpGet:
                          path: /healthz/live
                          port: 8080
        """.trimIndent()
        assertTrue(K8sProbeManifestScanner.scan(text).isEmpty())
    }

    @Test
    fun `container with readiness but no liveness probe is flagged once`() {
        val text = """
            kind: Deployment
            spec:
              template:
                spec:
                  containers:
                    - name: web
                      readinessProbe:
                        httpGet:
                          path: /healthz/ready
                          port: 8080
        """.trimIndent()
        val hits = K8sProbeManifestScanner.scan(text)
        assertEquals(1, hits.size)
        assertEquals(ProbeProblem.MISSING_LIVENESS_PROBE, hits[0].problem)
    }

    @Test
    fun `non-workload kind is never scanned`() {
        val text = """
            kind: ConfigMap
            data:
              containers: "not a real k8s container list"
        """.trimIndent()
        assertTrue(K8sProbeManifestScanner.scan(text).isEmpty())
    }

    @Test
    fun `Job kind is out of scope in v0_1`() {
        val text = """
            kind: Job
            spec:
              template:
                spec:
                  containers:
                    - name: batch-worker
                      image: acmecorp/batch:1.0
        """.trimIndent()
        assertTrue(K8sProbeManifestScanner.scan(text).isEmpty())
    }

    @Test
    fun `multiple containers are each checked independently`() {
        val text = """
            kind: Pod
            spec:
              containers:
                - name: web
                  readinessProbe:
                    httpGet:
                      path: /healthz/ready
                      port: 8080
                  livenessProbe:
                    httpGet:
                      path: /healthz/live
                      port: 8080
                - name: sidecar
                  image: acmecorp/sidecar:1.0
        """.trimIndent()
        val hits = K8sProbeManifestScanner.scan(text)
        assertEquals(2, hits.size)
        assertTrue(hits.all { it.containerLabel == "sidecar" })
    }
}
