package net.npg.wt2t.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.npg.wt2t.data.model.AppSettings
import net.npg.wt2t.domain.service.MockConfigRepository
import net.npg.wt2t.domain.service.MockProjectRepository
import net.npg.wt2t.domain.service.MockTaskRepository
import net.npg.wt2t.domain.service.MockTimeRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock

class BookingUseCaseTest {

    private class ConstantClock(private val time: kotlin.time.Instant) : Clock {
        override fun now(): kotlin.time.Instant = time
    }

    private suspend fun setupService(mergeThresholdMinutes: Int = 5): Pair<BookingUseCase, MockTimeRepository> {
        val projectRepo = MockProjectRepository()
        val taskRepo = MockTaskRepository()
        val timeRepo = MockTimeRepository()
        val configRepo = MockConfigRepository()
        configRepo.setInt(AppSettings.MERGE_THRESHOLD_MINUTES.name, mergeThresholdMinutes)
        val bookingUseCase = BookingUseCase(timeRepo, taskRepo, projectRepo, configRepo, ConstantClock(kotlin.time.Instant.fromEpochSeconds(0)))
        return bookingUseCase to timeRepo
    }

    @Test
    fun testStartBooking() = runTest {
        val (service, timeRepository) = setupService()
        val taskId = "task-1"
        val date = LocalDate(2023, 10, 27)
        val startTime = LocalTime(9, 0)

        service.startBooking(taskId, date, startTime)

        val times = timeRepository.getTimesForDate(date).first()
        assertEquals(1, times.size)
        assertEquals(taskId, times[0].taskId)
        assertEquals(startTime, times[0].start)
        assertNull(times[0].end)
    }

    @Test
    fun testOverlapHandling() = runTest {
        val (service, timeRepository) = setupService()
        val date = LocalDate(2023, 10, 27)

        service.startBooking("task-1", date, LocalTime(9, 0))
        service.startBooking("task-2", date, LocalTime(10, 0))

        val times = timeRepository.getTimesForDate(date).first()
        assertEquals(2, times.size)

        val t1 = times.find { it.taskId == "task-1" }
        val t2 = times.find { it.taskId == "task-2" }

        assertNotNull(t1?.end)
        assertEquals(LocalTime(10, 0), t1.end)
        assertNull(t2?.end)
    }

    @Test
    fun testMergeLogic() = runTest {
        val (service, timeRepository) = setupService()
        val date = LocalDate(2023, 10, 27)

        service.startBooking("task-1", date, LocalTime(9, 0))
        service.endBooking(date, LocalTime(10, 0))

        // Start next booking within 5 minutes (at 10:03)
        service.startBooking("task-2", date, LocalTime(10, 3))

        val times = timeRepository.getTimesForDate(date).first()
        val t2 = times.find { it.taskId == "task-2" }

        // Should have merged to 10:00
        assertEquals(LocalTime(10, 0), t2?.start)
    }

    @Test
    fun testNoMergeWhenExceedThreshold() = runTest {
        val (service, timeRepository) = setupService()
        val date = LocalDate(2023, 10, 27)

        service.startBooking("task-1", date, LocalTime(9, 0))
        service.endBooking(date, LocalTime(10, 0))

        // Start next booking after 6 minutes (at 10:06)
        service.startBooking("task-2", date, LocalTime(10, 6))

        val times = timeRepository.getTimesForDate(date).first()
        val t2 = times.find { it.taskId == "task-2" }

        // Should NOT have merged
        assertEquals(LocalTime(10, 6), t2?.start)
    }

    @Test
    fun testMergeLogicWithCustomThreshold() = runTest {
        val (service, timeRepository) = setupService(mergeThresholdMinutes = 10)
        val date = LocalDate(2023, 10, 27)

        // Set threshold to 10 minutes
        service.startBooking("task-1", date, LocalTime(9, 0))
        service.endBooking(date, LocalTime(10, 0))

        // Start next booking after 8 minutes (at 10:08) - should merge because 8 <= 10
        service.startBooking("task-2", date, LocalTime(10, 8))

        val times = timeRepository.getTimesForDate(date).first()
        val t2 = times.find { it.taskId == "task-2" }

        assertEquals(LocalTime(10, 0), t2?.start)
    }

}
