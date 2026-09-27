package net.npg.wt2t.domain.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Generates PDF reports with Android graphics APIs. */
class AndroidReportService(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher,
) : ReportService {
    /** Generates pdf report. */
    override suspend fun generatePdfReport(data: ReportData, fileName: String): String = withContext(dispatcher) {
        val destination = Uri.parse(fileName)
        val document = PdfDocument()
        try {
            val drawingSurface = AndroidReportDrawingSurface(document)
            ReportGenerator(data, drawingSurface).generate()
            val outputStream = checkNotNull(context.contentResolver.openOutputStream(destination, "w")) {
                "Could not open the selected PDF destination."
            }
            outputStream.use(document::writeTo)
        } finally {
            document.close()
        }
        destination.toString()
    }
}

/** Adapts an Android PDF canvas to report drawing operations. */
private class AndroidReportDrawingSurface(
    private val document: PdfDocument,
) : ReportDrawingSurface {
    private val paint = Paint()
    private var pageNumber = 0
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas

    /** Starts page. */
    override fun startPage() {
        pageNumber += 1
        val pageInfo = PdfDocument.PageInfo.Builder(
            ReportGenerator.PAGE_WIDTH,
            ReportGenerator.PAGE_HEIGHT,
            pageNumber,
        ).create()
        page = document.startPage(pageInfo)
        canvas = page.canvas
    }

    /** Finishes the current report page. */
    override fun finishPage() {
        document.finishPage(page)
    }

    /** Draws rectangle. */
    override fun drawRectangle(
        horizontalPosition: Float,
        verticalPosition: Float,
        width: Float,
        height: Float,
    ) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = TABLE_BORDER_WIDTH
        val top = ReportGenerator.PAGE_HEIGHT - verticalPosition - height
        canvas.drawRect(
            horizontalPosition,
            top,
            horizontalPosition + width,
            top + height,
            paint,
        )
    }

    /** Draws text. */
    override fun drawText(
        text: String,
        fontStyle: ReportFontStyle,
        textSize: Float,
        horizontalPosition: Float,
        baseline: Float,
    ) {
        configurePaint(fontStyle, textSize)
        canvas.drawText(
            text,
            horizontalPosition,
            ReportGenerator.PAGE_HEIGHT - baseline,
            paint,
        )
    }

    /** Measures text. */
    override fun measureText(text: String, fontStyle: ReportFontStyle, textSize: Float): Float {
        configurePaint(fontStyle, textSize)
        return paint.measureText(text)
    }

    /** Configures paint. */
    private fun configurePaint(fontStyle: ReportFontStyle, textSize: Float) {
        paint.style = Paint.Style.FILL
        paint.textSize = textSize
        paint.isFakeBoldText = fontStyle == ReportFontStyle.Bold
    }

    private companion object {
        const val TABLE_BORDER_WIDTH = 1f
    }
}
