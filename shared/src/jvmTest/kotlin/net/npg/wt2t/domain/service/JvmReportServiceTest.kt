package net.npg.wt2t.domain.service

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.apache.pdfbox.Loader
import org.apache.pdfbox.contentstream.operator.Operator
import org.apache.pdfbox.pdfparser.PDFStreamParser
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class JvmReportServiceTest {

    @Test
    fun `report renders daily overview as a table without pipe separators`() = runTest {
        val outputFile = File.createTempFile("worktime-report-", ".pdf")
        val inputData = createReportData()

        try {
            JvmReportService(StandardTestDispatcher(testScheduler))
                .generatePdfReport(inputData, outputFile.absolutePath)

            Loader.loadPDF(outputFile).use { document ->
                val actualText = PDFTextStripper().getText(document)
                val rectangleCount = PDFStreamParser(document.getPage(0)).parse()
                    .filterIsInstance<Operator>()
                    .count { it.name == RECTANGLE_OPERATOR }

                assertFalse(actualText.contains('|'))
                assertEquals(EXPECTED_CELL_COUNT, rectangleCount)
            }
        } finally {
            outputFile.delete()
        }
    }

    @Test
    fun `large report generation uses the injected dispatcher`() = runTest {
        val outputFile = File.createTempFile("worktime-large-report-", ".pdf")
        val dispatcher = RecordingDispatcher(StandardTestDispatcher(testScheduler))
        val inputData = createReportData(
            items = List(LARGE_REPORT_ITEM_COUNT) { index ->
                createReportItem(task = "Task $index")
            },
        )

        try {
            JvmReportService(dispatcher).generatePdfReport(inputData, outputFile.absolutePath)

            assertTrue(dispatcher.dispatchCount > 0)
            assertTrue(outputFile.length() > 0)
            Loader.loadPDF(outputFile).use { document ->
                assertTrue(document.numberOfPages > 1)
            }
        } finally {
            outputFile.delete()
        }
    }

    private fun createReportData(
        items: List<ReportItem> = listOf(createReportItem()),
    ) = ReportData(
        startDate = LocalDate(2026, 7, 31),
        endDate = LocalDate(2026, 7, 31),
        items = items,
        totalDuration = "01:00",
        overtime = "00:00",
        text = ReportText(
            title = "Time report",
            columnHeaders = ReportColumnHeaders(
                date = "Date",
                project = "Project",
                task = "Task",
                start = "Start",
                end = "End",
                duration = "Duration",
            ),
            totalTime = "Total time",
            overtime = "Overtime",
            unknownTask = "Unknown task",
        ),
    )

    private fun createReportItem(task: String = "Task") = ReportItem(
        date = LocalDate(2026, 7, 31),
        project = "Project",
        task = task,
        start = "08:00",
        end = "09:00",
        duration = "01:00",
        notes = emptyList(),
    )

    private companion object {
        const val RECTANGLE_OPERATOR = "re"
        const val EXPECTED_CELL_COUNT = 14
        const val LARGE_REPORT_ITEM_COUNT = 1_000
    }
}

private class RecordingDispatcher(
    private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {
    var dispatchCount = 0
        private set

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatchCount++
        delegate.dispatch(context, block)
    }
}
