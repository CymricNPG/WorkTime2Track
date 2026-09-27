package net.npg.wt2t.domain.service

/** Defines operations for synchronizing and backing up cloud data. */
interface CloudStorageService {
    /** Synchronizes data with cloud storage. */
    suspend fun sync()
    /** Creates a backup in cloud storage. */
    suspend fun backup()
}

/** Provides a no-op cloud storage implementation. */
class DummyCloudStorageService : CloudStorageService {
    /** Synchronizes data with cloud storage. */
    override suspend fun sync() {
        // Preparation for Phase 5.4
    }

    /** Creates a backup in cloud storage. */
    override suspend fun backup() {
        // Preparation for Phase 5.4
    }
}
