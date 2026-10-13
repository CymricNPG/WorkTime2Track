package net.npg.wt2t.domain.service

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class JvmDatabaseFileServiceTest {

    @Test
    fun `large backup round trip uses the injected dispatcher`() = runTest {
        val backupFile = Files.createTempFile("worktime-large-backup-", ".json").toFile()
        val dispatcher = FileRecordingDispatcher(StandardTestDispatcher(testScheduler))
        val service = JvmDatabaseFileService(dispatcher)
        val expectedContent = ByteArray(LARGE_BACKUP_SIZE_BYTES) { index -> index.toByte() }

        try {
            service.write(expectedContent, backupFile.absolutePath)
            val actualContent = service.read(backupFile.absolutePath)

            assertTrue(dispatcher.dispatchCount >= EXPECTED_OPERATION_COUNT)
            assertContentEquals(expectedContent, actualContent)
        } finally {
            backupFile.delete()
        }
    }

    private companion object {
        const val LARGE_BACKUP_SIZE_BYTES = 8 * 1024 * 1024
        const val EXPECTED_OPERATION_COUNT = 2
    }
}

private class FileRecordingDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {
    var dispatchCount = 0
        private set

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatchCount++
        delegate.dispatch(context, block)
    }
}
