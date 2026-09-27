package net.npg.wt2t.domain.usecase

import net.npg.wt2t.data.repository.DatabaseExportRepository
import net.npg.wt2t.data.repository.DatabaseManagementRepository

/** Coordinates portable backup import, export, and complete database recreation. */
class BackupUseCase(
    private val databaseExportRepository: DatabaseExportRepository,
    private val databaseManagementRepository: DatabaseManagementRepository,
) {
    suspend fun export(): ByteArray = databaseExportRepository.exportToJson().encodeToByteArray()
    suspend fun import(content: ByteArray) = databaseExportRepository.importFromJsonContent(content.decodeToString())
    suspend fun recreateDatabase() = databaseManagementRepository.recreateDatabase()
}
