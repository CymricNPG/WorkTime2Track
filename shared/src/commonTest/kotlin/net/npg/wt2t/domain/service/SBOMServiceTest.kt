package net.npg.wt2t.domain.service

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.npg.wt2t.testing.TEST_SBOM_BYTES
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SBOMServiceTest {
    @Test
    fun loadsComponentsFromResource() = runTest {
        val service = SBOMService(StandardTestDispatcher(testScheduler)) { TEST_SBOM_BYTES }

        val actualComponents = service.loadComponents()

        assertTrue(actualComponents.isNotEmpty())
        assertTrue(actualComponents.none { component -> "type=pom" in component.packageUrl })
    }

    @Test
    fun parsesComponentsAndDependencyTypes() {
        val inputJson = """
            {
              "bomFormat": "CycloneDX",
              "specVersion": "1.6",
              "version": 1,
              "components": [
                {
                  "type": "application",
                  "bom-ref": "pkg:project?project_path=app",
                  "name": "app",
                  "version": "1",
                  "purl": "pkg:project?project_path=app"
                },
                {
                  "type": "library",
                  "bom-ref": "pkg:maven/example/direct@1.0",
                  "name": "direct",
                  "version": "1.0",
                  "licenses": [{"license": {"id": "Apache-2.0"}}],
                  "purl": "pkg:maven/example/direct@1.0"
                },
                {
                  "type": "library",
                  "bom-ref": "pkg:maven/example/transitive@2.0",
                  "name": "transitive",
                  "version": "2.0",
                  "licenses": [{"license": {"name": "MIT"}}],
                  "purl": "pkg:maven/example/transitive@2.0"
                },
                {
                  "type": "library",
                  "bom-ref": "pkg:maven/example/platform@3.0?classifier=desktop&type=pom",
                  "name": "platform",
                  "version": "3.0",
                  "licenses": [{"license": {"name": "MIT"}}],
                  "purl": "pkg:maven/example/platform@3.0?classifier=desktop&type=pom"
                }
              ],
              "dependencies": [
                {
                  "ref": "pkg:project?project_path=app",
                  "dependsOn": ["pkg:maven/example/direct@1.0"]
                },
                {
                  "ref": "pkg:maven/example/direct@1.0",
                  "dependsOn": ["pkg:maven/example/transitive@2.0"]
                }
              ]
            }
        """.trimIndent().encodeToByteArray()

        val actualComponents = parseCycloneDxJson(inputJson)

        assertEquals(2, actualComponents.size)
        assertEquals("Apache-2.0", actualComponents[0].license)
        assertTrue(actualComponents[0].isDirect)
        assertEquals("MIT", actualComponents[1].license)
        assertFalse(actualComponents[1].isDirect)
    }
}
