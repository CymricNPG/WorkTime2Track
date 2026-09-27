package net.npg.wt2t.domain.usecase

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.npg.wt2t.data.model.AppSettings
import net.npg.wt2t.testing.IntegrationTestEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EndOfDayUseCaseTest {

    @Test
    fun `configured target is loaded when day has no persisted value`() = runTest {
        IntegrationTestEnvironment.create().use { environment ->
            val inputDate = LocalDate(2023, 10, 27)
            environment.setConfig(AppSettings.DEFAULT_TARGET_HOURS, 7)
            environment.setConfig(AppSettings.DEFAULT_TARGET_MINUTES, 30)

            val actualData = environment.endOfDayUseCase.load(inputDate)

            assertEquals(450, actualData.targetMinutes)
            assertEquals(0, actualData.breakMinutes)
        }
    }

    @Test
    fun `existing daily values take precedence over configured target`() = runTest {
        IntegrationTestEnvironment.create().use { environment ->
            val inputDate = LocalDate(2023, 10, 27)
            environment.setConfig(AppSettings.DEFAULT_TARGET_HOURS, 7)
            environment.setConfig(AppSettings.DEFAULT_TARGET_MINUTES, 30)
            environment.createDailyWorkTime(inputDate, minutes = 480)
            val existing = environment.dailyWorkTimeRepository.getDailyWorkTime(inputDate).first()
            assertNotNull(existing)
            environment.dailyWorkTimeRepository.saveDailyWorkTime(existing.copy(breakMinutes = 45))

            val actualData = environment.endOfDayUseCase.load(inputDate)

            assertEquals(480, actualData.targetMinutes)
            assertEquals(45, actualData.breakMinutes)
        }
    }

    @Test
    fun `completion without bookings is rejected without creating daily values`() = runTest {
        IntegrationTestEnvironment.create().use { environment ->
            val inputDate = LocalDate(2023, 10, 27)

            assertFailsWith<IllegalStateException> {
                environment.endOfDayUseCase.complete(
                    date = inputDate,
                    endTime = LocalTime(17, 0),
                    targetMinutes = 420,
                    breakMinutes = 30,
                )
            }

            assertNull(environment.dailyWorkTimeRepository.getDailyWorkTime(inputDate).first())
        }
    }

    @Test
    fun `completion closes active booking and saves daily values`() = runTest {
        IntegrationTestEnvironment.create().use { environment ->
            val inputDate = LocalDate(2023, 10, 27)
            val inputTask = environment.createTask("Tracked task")
            val inputTime = environment.createTime(
                task = inputTask,
                date = inputDate,
                start = LocalTime(9, 0),
            )

            val actualDailyWorkTime = environment.endOfDayUseCase.complete(
                date = inputDate,
                endTime = LocalTime(17, 0),
                targetMinutes = 420,
                breakMinutes = 45,
            )

            val actualTime = environment.timeRepository.getTimesForDate(inputDate).first().single()
            assertEquals(inputTime.id, actualTime.id)
            assertEquals(LocalTime(17, 0), actualTime.end)
            assertEquals(420, actualDailyWorkTime.minutes)
            assertEquals(45, actualDailyWorkTime.breakMinutes)
            assertEquals(actualDailyWorkTime, environment.dailyWorkTimeRepository.getDailyWorkTime(inputDate).first())
        }
    }

    @Test
    fun `completion accepts a zero-minute active booking`() = runTest {
        IntegrationTestEnvironment.create().use { environment ->
            val inputDate = LocalDate(2023, 10, 27)
            val inputTask = environment.createTask("Tracked task")
            environment.createTime(inputTask, inputDate, LocalTime(17, 0))

            environment.endOfDayUseCase.complete(
                date = inputDate,
                endTime = LocalTime(17, 0),
                targetMinutes = 420,
                breakMinutes = 45,
            )

            assertEquals(LocalTime(17, 0), environment.timeRepository.getTimesForDate(inputDate).first().single().end)
        }
    }

    @Test
    fun `completion keeps existing daily value identity`() = runTest {
        IntegrationTestEnvironment.create().use { environment ->
            val inputDate = LocalDate(2023, 10, 27)
            val inputTask = environment.createTask("Tracked task")
            environment.createTime(inputTask, inputDate, LocalTime(9, 0), LocalTime(12, 0))
            val existing = environment.createDailyWorkTime(inputDate, minutes = 480)

            val actualDailyWorkTime = environment.endOfDayUseCase.complete(
                date = inputDate,
                endTime = LocalTime(17, 0),
                targetMinutes = 420,
                breakMinutes = 45,
            )

            assertEquals(existing.id, actualDailyWorkTime.id)
            assertEquals(420, actualDailyWorkTime.minutes)
            assertEquals(45, actualDailyWorkTime.breakMinutes)
        }
    }

    @Test
    fun `daily value save failure rolls back active booking closure`() = runTest {
        IntegrationTestEnvironment.create().use { environment ->
            val inputDate = LocalDate(2023, 10, 27)
            val inputTask = environment.createTask("Tracked task")
            val inputTime = environment.createTime(
                task = inputTask,
                date = inputDate,
                start = LocalTime(9, 0),
            )

            assertFails {
                environment.endOfDayUseCase.complete(
                    date = inputDate,
                    endTime = LocalTime(17, 0),
                    targetMinutes = 1_441,
                    breakMinutes = 30,
                )
            }

            val actualTime = environment.timeRepository.getTimesForDate(inputDate).first().single()
            assertEquals(inputTime.id, actualTime.id)
            assertNull(actualTime.end)
            assertNull(environment.dailyWorkTimeRepository.getDailyWorkTime(inputDate).first())
        }
    }
}
