package net.npg.wt2t.domain.service

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Reads and writes Android database backups through the content resolver. */
class AndroidDatabaseFileService(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher,
) : DatabaseFileService {
    /** Writes backup content to a content URI. */
    override suspend fun write(content: ByteArray, destination: String): Unit = withContext(dispatcher) {
        val uri = Uri.parse(destination)
        requireNotNull(context.contentResolver.openOutputStream(uri)).use { output ->
            output.write(content)
        }
    }

    /** Reads backup content from a content URI. */
    override suspend fun read(source: String): ByteArray = withContext(dispatcher) {
        val uri = Uri.parse(source)
        requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
            input.readBytes()
        }
    }
}
