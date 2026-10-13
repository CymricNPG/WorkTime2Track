package net.npg.wt2t.domain.service

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File

/** Reads and writes desktop database backup files. */
class JvmDatabaseFileService(
    private val dispatcher: CoroutineDispatcher,
) : DatabaseFileService {
    /** Writes backup content to a file. */
    override suspend fun write(content: ByteArray, destination: String): Unit = withContext(dispatcher) {
        File(destination).writeBytes(content)
    }

    /** Reads backup content from a file. */
    override suspend fun read(source: String): ByteArray = withContext(dispatcher) {
        File(source).readBytes()
    }
}
