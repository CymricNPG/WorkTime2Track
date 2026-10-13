package net.npg.wt2t.domain.service

/** Describes a database backup that should be saved by the platform file picker. */
data class DatabaseExportRequest(
    val fileName: String,
    val content: ByteArray,
    val onFinished: (Result<Boolean>) -> Unit,
)

/** Requests database backup content from the platform file picker. */
data class DatabaseImportRequest(
    val onFinished: (Result<ByteArray?>) -> Unit,
)
