package net.npg.wt2t.domain.service

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class BundledSBOMServiceTest {
    @Test
    fun loadsBundledSbom() = runTest {
        val service = SBOMService(StandardTestDispatcher(testScheduler))

        val actualComponents = service.loadComponents()

        assertTrue(actualComponents.isNotEmpty())
        assertTrue(actualComponents.none { component -> "type=pom" in component.packageUrl })
    }
}
