package net.npg.wt2t.domain.service

import kotlinx.datetime.LocalDate

/** Contains the rows and totals required to generate a report. */
data class ReportData(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val items: List<ReportItem>,
    val totalDuration: String,
    val overtime: String,
    val text: ReportText
)

/** Contains localized text displayed in a report. */
data class ReportText(
    val title: String,
    val columnHeaders: ReportColumnHeaders,
    val totalTime: String,
    val overtime: String,
    val unknownTask: String
)

/** Contains localized report table column headings. */
data class ReportColumnHeaders(
    val date: String,
    val project: String,
    val task: String,
    val start: String,
    val end: String,
    val duration: String,
) {
    /** Returns the column headers in report display order. */
    internal fun asList(): List<String> = listOf(date, project, task, start, end, duration)
}

/** Represents one time-booking row in a report. */
data class ReportItem(
    val date: LocalDate,
    val project: String,
    val task: String,
    val start: String,
    val end: String,
    val duration: String,
    val notes: List<String>,
)

/** Contains the output name and report content for a PDF export. */
data class PdfExportRequest(
    val data: ReportData,
    val fileName: String,
)

/** Defines platform-specific PDF report generation. */
interface ReportService {
    /** Generates pdf report. */
    suspend fun generatePdfReport(data: ReportData, fileName: String): String
}
