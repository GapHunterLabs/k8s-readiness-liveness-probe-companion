package dev.gaphunter.k8sreadinesslivenessprobecompanion.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MissingProbeInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(MissingProbeInspection::class.java)
    }

    fun `test a container with no probes produces warnings`() {
        myFixture.configureByText(
            "deployment.yaml",
            """
            kind: Deployment
            spec:
              template:
                spec:
                  containers:
                    - name: web
                      image: acmecorp/web:1.0
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("readinessProbe") == true })
        assertTrue(highlights.any { it.description?.contains("livenessProbe") == true })
    }

    fun `test a container with both probes produces no warning`() {
        myFixture.configureByText(
            "deployment.yaml",
            """
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
                      livenessProbe:
                        httpGet:
                          path: /healthz/live
                          port: 8080
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("web") == true })
    }

    fun `test a non-yaml file is never scanned`() {
        myFixture.configureByText(
            "Notes.java",
            "String x = \"kind: Deployment\\ncontainers:\\n  - name: web\";",
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("readinessProbe") == true })
    }
}
