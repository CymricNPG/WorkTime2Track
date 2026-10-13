package net.npg.wt2t.testing

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class IntegrationTestEnvironmentTest {

    @Test
    fun createsRelatedDataAndCallsServices() = runTest {
        IntegrationTestEnvironment.create().use { environment ->
            val project = environment.createProject("Integration project")
            val task = environment.createTask("Integration task", project)
            val date = LocalDate(2026, 6, 21)

            environment.bookingUseCase.startBooking(task.id, date, LocalTime(8, 30))
            environment.bookingUseCase.endBooking(date, LocalTime(10, 0))
            environment.createDailyWorkTime(date, minutes = 480)

            val storedTime = environment.timeRepository.getTimesForDate(date).first().single()

            assertEquals(task.id, storedTime.taskId)
            assertEquals(LocalTime(8, 30), storedTime.start)
            assertEquals(LocalTime(10, 0), storedTime.end)
            assertNotNull(environment.dailyWorkTimeRepository.getDailyWorkTime(date).first())
        }
    }

    @Test
    fun persistentDatabaseCanBeReopenedWithoutResettingData() = runTest {
        val databasePath = createTempDirectory()
            .resolve("desktop-integration.db")
            .toAbsolutePath()
            .toString()

        IntegrationTestEnvironment.create(
            IntegrationTestDatabase.Persistent(databasePath),
        ).use { environment ->
            environment.createProject("Desktop-visible project")
        }

        IntegrationTestEnvironment.create(
            IntegrationTestDatabase.Persistent(
                path = databasePath,
                resetBeforeTest = false,
            ),
        ).use { environment ->
            val projects = environment.projectManagementUseCase.getAllProjects().first()

            assertEquals("Desktop-visible project", projects.single().name)
            assertEquals(databasePath, environment.databasePath)
        }
    }


    @Test
    fun persistentDatabaseWitMoreEntries() = runTest {
        val databasePath = createTempDirectory()
            .resolve("desktop-integration.db")
            .toAbsolutePath()
            .toString()
        Logger.i { "Persistent database stored in $databasePath" }
        IntegrationTestEnvironment.create(
            IntegrationTestDatabase.Persistent(databasePath),
        ).use { environment ->
            val project1 = environment.createProject("Project A")
            val project2 = environment.createProject("Project B")
            val task1 = environment.createTask("Task A", project1)
            val task2 = environment.createTask("Task B", project1)
            val task3 = environment.createTask("Task C", project2)
            val task4 = environment.createTask("Task D", project2)
            val freeTask = environment.createTask("Free")
            environment.createTime(
                task1,
                LocalDate(2023, 10, 27),
                LocalTime(9, 0),
                LocalTime(10, 0)
            )
            environment.createTime(
                task2,
                LocalDate(2023, 10, 27),
                LocalTime(10, 0),
                LocalTime(13, 0)
            )
            environment.createTime(
                task3,
                LocalDate(2023, 10, 28),
                LocalTime(9, 0),
                LocalTime(10, 0)
            )
            environment.createTime(
                freeTask,
                LocalDate(2023, 10, 28),
                LocalTime(10, 0),
                LocalTime(13, 0)
            )
            environment.createTime(
                task4,
                LocalDate(2023, 10, 28),
                LocalTime(13, 0),
                LocalTime(15, 0)
            )
            environment.createTime(
                task1,
                LocalDate(2023, 10, 28),
                LocalTime(15, 0),
                LocalTime(17, 0),
                listOf("This a note 1", "This a note 2")
            )
            environment.createDailyWorkTime(LocalDate(2023, 10, 27), minutes = 480)
            environment.createDailyWorkTime(LocalDate(2023, 10, 28), minutes = 480)
        }
    }
}
