package minthive.labelmaker

import cats.effect.SyncIO
import com.itextpdf.kernel.font.{PdfFont, PdfFontFactory}
import com.itextpdf.kernel.pdf.PdfDocument

case class Fonts(normal: PdfFont, bold: PdfFont) {
  def addToDoc(pdfDoc: PdfDocument): SyncIO[Unit] = SyncIO {
    pdfDoc.addFont(normal)
    pdfDoc.addFont(bold)
  }
}
object Fonts {
  def create = SyncIO[Fonts] {
    Fonts(
      PdfFontFactory.createFont("fonts/LiberationSans-Regular.ttf", "UTF-8"),
      PdfFontFactory.createFont("fonts/LiberationSans-Bold.ttf", "UTF-8")
    )
  }
}
