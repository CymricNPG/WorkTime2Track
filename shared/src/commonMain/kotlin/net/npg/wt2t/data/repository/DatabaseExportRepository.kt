package net.npg.wt2t.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.npg.wt2t.data.model.DailyWorkTime
import net.npg.wt2t.data.model.Project
import net.npg.wt2t.data.model.Task
import net.npg.wt2t.data.model.Time
import net.npg.wt2t.data.model.atMinutePrecision
import net.npg.wt2t.db.WorkTimeDatabase

/** Contains a complete portable snapshot of persisted application data. */
@Serializable
data class DatabaseExport(
    val formatVersion: Int = CURRENT_EXPORT_FORMAT_VERSION,
    val projects: List<Project>,
    val tasks: List<Task>,
    val times: List<Time>,
    val dailyWorkTimes: List<DailyWorkTime>,
    val configs: Map<String, String> = emptyMap(),
)

/** Exports and imports complete database snapshots as JSON. */
class DatabaseExportRepository(
    private val db: WorkTimeDatabase,
    private val dispatcher: CoroutineDispatcher,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    /** Serializes all persisted application data to JSON. */
    suspend fun exportToJson(): String = withContext(dispatcher) {
        val export = DatabaseExport(
            projects = db.workTimeDatabaseQueries.selectAllProjects().executeAsList().map { it.toModel() },
            tasks = db.workTimeDatabaseQueries.selectAllTasks().executeAsList().map { it.toModel() },
            times = db.workTimeDatabaseQueries.selectAllTimes().executeAsList().map { it.toModel() },
            dailyWorkTimes = db.workTimeDatabaseQueries.selectAllDailyWorkTimes().executeAsList().map { it.toModel() },
            configs = db.workTimeDatabaseQueries.selectAllConfigs().executeAsList().associate { it.key to it.value_ },
        )
        json.encodeToString(export)
    }

    /** Atomically replaces persisted application data with the supplied JSON content. */
    suspend fun importFromJsonContent(content: String): Unit = withContext(dispatcher) {
        val decodedExport = try {
            json.decodeFromString<DatabaseExport>(content)
        } catch (exception: Exception) {
            throw DatabaseImportException(
                failure = DatabaseImportFailure.MALFORMED_CONTENT,
                message = "Database backup content is malformed",
                cause = exception,
            )
        }
        val export = decodedExport.copy(times = decodedExport.times.map(Time::atMinutePrecision))
        if (export.formatVersion !in 1..CURRENT_EXPORT_FORMAT_VERSION) {
            throw DatabaseImportException(
                failure = DatabaseImportFailure.UNSUPPORTED_FORMAT,
                message = "Unsupported database export format ${export.formatVersion}",
            )
        }
        DatabaseImportValidator.validate(export)

        try {
            db.workTimeDatabaseQueries.transaction {
                deleteExistingData()
                insertExport(export)
            }
        } catch (exception: Exception) {
            throw DatabaseImportException(
                failure = DatabaseImportFailure.PERSISTENCE_FAILED,
                message = "Database backup could not be persisted",
                cause = exception,
            )
        }
    }

    private fun deleteExistingData() {
        db.workTimeDatabaseQueries.deleteAllTimes()
        db.workTimeDatabaseQueries.deleteAllTasks()
        db.workTimeDatabaseQueries.deleteAllProjects()
        db.workTimeDatabaseQueries.deleteAllDailyWorkTimes()
        db.workTimeDatabaseQueries.deleteAllConfigs()
    }

    private fun insertExport(export: DatabaseExport) {
        export.projects.forEach {
            db.workTimeDatabaseQueries.insertProject(it.id, it.name, it.closed)
        }
        export.tasks.forEach {
            db.workTimeDatabaseQueries.insertTask(it.id, it.name, it.freeTime, it.closed, it.projectId)
        }
        export.times.forEach {
            db.workTimeDatabaseQueries.insertTime(
                it.id,
                it.taskId,
                it.date.toString(),
                it.start.toString(),
                it.end?.toString(),
                Json.encodeToString(it.description),
            )
        }
        export.dailyWorkTimes.forEach {
            db.workTimeDatabaseQueries.insertDailyWorkTime(
                it.id,
                it.date.toString(),
                it.minutes.toLong(),
                it.breakMinutes.toLong(),
            )
        }
        export.configs.forEach { (key, value) ->
            db.workTimeDatabaseQueries.insertConfig(key, value)
        }
    }
}

private const val CURRENT_EXPORT_FORMAT_VERSION = 1
