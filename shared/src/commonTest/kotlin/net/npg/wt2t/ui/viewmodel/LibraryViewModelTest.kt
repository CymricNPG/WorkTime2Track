package net.npg.wt2t.ui.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.npg.wt2t.domain.service.SBOMService
import net.npg.wt2t.testing.TEST_SBOM_BYTES
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    @Test
    fun `loads bundled components into state`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val viewModel = LibraryViewModel(SBOMService(testDispatcher) { TEST_SBOM_BYTES })

            assertTrue(viewModel.uiState.value.components == null)

            advanceUntilIdle()

            assertTrue(assertNotNull(viewModel.uiState.value.components).isNotEmpty())
            assertFalse(viewModel.uiState.value.hasLoadFailed)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
