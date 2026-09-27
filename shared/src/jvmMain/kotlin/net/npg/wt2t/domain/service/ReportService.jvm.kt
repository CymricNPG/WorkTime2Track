package net.npg.wt2t.domain.service

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import java.io.File

/** Generates PDF reports with Apache PDFBox. */
class JvmReportService(
    private val dispatcher: CoroutineDispatcher,
) : ReportService {
    /** Generates pdf report. */
    override suspend fun generatePdfReport(data: ReportData, fileName: String): String = withContext(dispatcher) {
        PDDocument().use { document ->
            val drawingSurface = PdfBoxReportDrawingSurface(document)
            ReportGenerator(data, drawingSurface).generate()

            val file = File(fileName)
            document.save(file)
            file.absolutePath
        }
    }
}

/** Adapts a PDFBox document to report drawing operations. */
private class PdfBoxReportDrawingSurface(
    private val document: PDDocument,
) : ReportDrawingSurface {
    private val regularFont = PDType1Font(Standard14Fonts.FontName.HELVETICA)
    private val boldFont = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
    private lateinit var contentStream: PDPageContentStream

    /** Starts page. */
    override fun startPage() {
        val page = PDPage()
        document.addPage(page)
        contentStream = PDPageContentStream(document, page)
    }

    /** Finishes the current report page. */
    override fun finishPage() {
        contentStream.close()
    }

    /** Draws rectangle. */
    override fun drawRectangle(
        horizontalPosition: Float,
        verticalPosition: Float,
        width: Float,
        height: Float,
    ) {
        contentStream.addRect(horizontalPosition, verticalPosition, width, height)
        contentStream.stroke()
    }

    /** Draws text. */
    override fun drawText(
        text: String,
        fontStyle: ReportFontStyle,
        textSize: Float,
        horizontalPosition: Float,
        baseline: Float,
    ) {
        contentStream.beginText()
        contentStream.setFont(getFont(fontStyle), textSize)
        contentStream.newLineAtOffset(horizontalPosition, baseline)
        contentStream.showText(text)
        contentStream.endText()
    }

    /** Measures text. */
    override fun measureText(text: String, fontStyle: ReportFontStyle, textSize: Float): Float {
        return getFont(fontStyle).getStringWidth(text) / FONT_WIDTH_SCALE * textSize
    }

    /** Returns font. */
    private fun getFont(fontStyle: ReportFontStyle): PDType1Font = when (fontStyle) {
        ReportFontStyle.Regular -> regularFont
        ReportFontStyle.Bold -> boldFont
    }

    private companion object {
        const val FONT_WIDTH_SCALE = 1000f
    }
}
