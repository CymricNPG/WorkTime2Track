package net.npg.wt2t.domain.service

/** Reads and writes database backup content without blocking the caller's dispatcher. */
interface DatabaseFileService {
    /** Writes backup content to a platform destination. */
    suspend fun write(content: ByteArray, destination: String)

    /** Reads backup content from a platform source. */
    suspend fun read(source: String): ByteArray
}
