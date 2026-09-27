package net.npg.wt2t.domain.service

/** Defines the font styles supported by report drawing surfaces. */
internal enum class ReportFontStyle {
    Regular,
    Bold,
}

/** Defines platform-independent drawing operations used to render reports. */
internal interface ReportDrawingSurface {
    /** Starts page. */
    fun startPage()

    /** Finishes the current report page. */
    fun finishPage()

    /** Draws rectangle. */
    fun drawRectangle(
        horizontalPosition: Float,
        verticalPosition: Float,
        width: Float,
        height: Float,
    )

    /** Draws text. */
    fun drawText(
        text: String,
        fontStyle: ReportFontStyle,
        textSize: Float,
        horizontalPosition: Float,
        baseline: Float,
    )

    /** Measures text. */
    fun measureText(text: String, fontStyle: ReportFontStyle, textSize: Float): Float
}

/** Lays out report data on a platform-specific drawing surface. */
internal class ReportGenerator(
    private val data: ReportData,
    private val drawingSurface: ReportDrawingSurface,
) {
    private var verticalPosition = TOP_POSITION

    /** Draws the complete report on the configured surface. */
    fun generate() {
        startPage(showTitle = true)

        data.items.forEach(::drawReportItem)
        drawSummary()

        drawingSurface.finishPage()
    }

    /** Draws report item. */
    private fun drawReportItem(item: ReportItem) {
        val noteLines = item.notes.flatMap(::wrapText)
        val requiredHeight = TABLE_ROW_HEIGHT + noteLines.size * NOTE_ROW_HEIGHT

        if (verticalPosition - requiredHeight < BOTTOM_POSITION) {
            startNextPage()
        }

        drawTableRow(
            values = listOf(
                item.date.toString(),
                item.project,
                item.task,
                item.start,
                item.end,
                item.duration,
            ),
            widths = TABLE_COLUMN_WIDTHS,
            height = TABLE_ROW_HEIGHT,
            fontStyle = ReportFontStyle.Regular,
            textSize = ENTRY_TEXT_SIZE,
        )

        noteLines.forEach(::drawNote)
    }

    /** Draws note. */
    private fun drawNote(note: String) {
        if (verticalPosition - NOTE_ROW_HEIGHT < BOTTOM_POSITION) {
            startNextPage()
        }
        drawTableRow(
            values = listOf(note),
            widths = NOTE_COLUMN_WIDTHS,
            height = NOTE_ROW_HEIGHT,
            fontStyle = ReportFontStyle.Regular,
            textSize = NOTE_TEXT_SIZE,
        )
    }

    /** Draws summary. */
    private fun drawSummary() {
        if (verticalPosition - TOTALS_SPACING - TABLE_ROW_HEIGHT < BOTTOM_POSITION) {
            startNextPage()
        }
        verticalPosition -= TOTALS_SPACING
        drawTableRow(
            values = listOf(
                "${data.text.totalTime}: ${data.totalDuration}",
                "${data.text.overtime}: ${data.overtime}",
            ),
            widths = SUMMARY_COLUMN_WIDTHS,
            height = TABLE_ROW_HEIGHT,
            fontStyle = ReportFontStyle.Bold,
            textSize = HEADER_TEXT_SIZE,
        )
    }

    /** Starts page. */
    private fun startPage(showTitle: Boolean) {
        drawingSurface.startPage()
        verticalPosition = TOP_POSITION

        if (showTitle) {
            drawingSurface.drawText(
                text = data.text.title,
                fontStyle = ReportFontStyle.Bold,
                textSize = TITLE_TEXT_SIZE,
                horizontalPosition = LEFT_MARGIN,
                baseline = verticalPosition,
            )
            verticalPosition -= TITLE_SPACING
        }
        drawTableRow(
            values = data.text.columnHeaders.asList(),
            widths = TABLE_COLUMN_WIDTHS,
            height = TABLE_HEADER_HEIGHT,
            fontStyle = ReportFontStyle.Bold,
            textSize = HEADER_TEXT_SIZE,
        )
    }

    /** Starts next page. */
    private fun startNextPage() {
        drawingSurface.finishPage()
        startPage(showTitle = false)
    }

    /** Draws table row. */
    private fun drawTableRow(
        values: List<String>,
        widths: List<Float>,
        height: Float,
        fontStyle: ReportFontStyle,
        textSize: Float,
    ) {
        var horizontalPosition = LEFT_MARGIN
        values.zip(widths).forEach { (value, width) ->
            drawingSurface.drawRectangle(
                horizontalPosition = horizontalPosition,
                verticalPosition = verticalPosition - height,
                width = width,
                height = height,
            )
            drawingSurface.drawText(
                text = fitText(value, fontStyle, textSize, width - 2 * TABLE_CELL_PADDING),
                fontStyle = fontStyle,
                textSize = textSize,
                horizontalPosition = horizontalPosition + TABLE_CELL_PADDING,
                baseline = verticalPosition - (height + textSize) / 2f + TEXT_BASELINE_ADJUSTMENT,
            )
            horizontalPosition += width
        }
        verticalPosition -= height
    }

    /** Wraps text. */
    private fun wrapText(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return text.lineSequence().flatMap { paragraph ->
            val words = paragraph.split(WHITESPACE_PATTERN)
            buildList {
                var line = ""
                words.forEach { word ->
                    val candidate = if (line.isEmpty()) word else "$line $word"
                    if (line.isNotEmpty() && measureText(candidate, ReportFontStyle.Regular, NOTE_TEXT_SIZE) > NOTE_WIDTH) {
                        add(line)
                        line = word
                    } else {
                        line = candidate
                    }
                }
                if (line.isNotEmpty()) add(line)
            }.asSequence()
        }.toList()
    }

    /** Fits text. */
    private fun fitText(
        text: String,
        fontStyle: ReportFontStyle,
        textSize: Float,
        maximumWidth: Float,
    ): String {
        if (measureText(text, fontStyle, textSize) <= maximumWidth) return text
        var endIndex = text.length
        while (
            endIndex > 0 &&
            measureText(text.take(endIndex) + ELLIPSIS, fontStyle, textSize) > maximumWidth
        ) {
            endIndex -= 1
        }
        return text.take(endIndex) + ELLIPSIS
    }

    /** Measures text. */
    private fun measureText(text: String, fontStyle: ReportFontStyle, textSize: Float): Float {
        return drawingSurface.measureText(text, fontStyle, textSize)
    }

    internal companion object {
        const val PAGE_WIDTH = 612
        const val PAGE_HEIGHT = 792
        private const val LEFT_MARGIN = 50f
        private const val PAGE_RIGHT = 545f
        private const val CONTENT_WIDTH = PAGE_RIGHT - LEFT_MARGIN
        private const val TOP_POSITION = 750f
        private const val BOTTOM_POSITION = 50f
        private const val TITLE_TEXT_SIZE = 18f
        private const val HEADER_TEXT_SIZE = 12f
        private const val ENTRY_TEXT_SIZE = 10f
        private const val NOTE_TEXT_SIZE = 9f
        private const val TITLE_SPACING = 50f
        private const val TOTALS_SPACING = 20f
        private const val TABLE_HEADER_HEIGHT = 24f
        private const val TABLE_ROW_HEIGHT = 22f
        private const val NOTE_ROW_HEIGHT = 18f
        private const val TABLE_CELL_PADDING = 4f
        private const val TEXT_BASELINE_ADJUSTMENT = 2f
        private const val NOTE_WIDTH = CONTENT_WIDTH - 2 * TABLE_CELL_PADDING
        private const val ELLIPSIS = "..."
        private val WHITESPACE_PATTERN = Regex("\\s+")
        private val TABLE_COLUMN_WIDTHS = listOf(70f, 100f, 120f, 55f, 55f, 95f)
        private val NOTE_COLUMN_WIDTHS = listOf(CONTENT_WIDTH)
        private val SUMMARY_COLUMN_WIDTHS = listOf(CONTENT_WIDTH / 2f, CONTENT_WIDTH / 2f)
    }
}
