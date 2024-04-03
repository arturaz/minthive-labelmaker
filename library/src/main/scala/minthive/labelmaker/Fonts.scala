package minthive.labelmaker

import com.itextpdf.kernel.font.{PdfFont, PdfFontFactory}
import com.itextpdf.kernel.pdf.PdfDocument
import zio.{Task, ZIO}

case class Fonts(normal: PdfFont, bold: PdfFont) {
  def addToDoc(pdfDoc: PdfDocument): Task[Unit] = ZIO.attempt {
    pdfDoc.addFont(normal)
    pdfDoc.addFont(bold)
  }
}
object Fonts {
  def create: Task[Fonts] = ZIO.attemptBlocking {
    Fonts(
      PdfFontFactory.createFont("fonts/LiberationSans-Regular.ttf", "UTF-8"),
      PdfFontFactory.createFont("fonts/LiberationSans-Bold.ttf", "UTF-8")
    )
  }
}
