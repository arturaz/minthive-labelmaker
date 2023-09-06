package minthive.labelmaker

import cats.data.ValidatedNec
import cats.effect.SyncIO
import cats.syntax.all.*
import com.github.tototoshi.csv.CSVReader
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.geom.{PageSize, Rectangle}
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.{PdfDocument, PdfPage}
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.{Paragraph, Text}
import com.itextpdf.layout.properties.{TextAlignment, VerticalAlignment}

import java.nio.file.Path

object LabelDataCSVReader {
  def readCSV(path: Path): SyncIO[ValidatedNec[String, Vector[LabelData]]] = SyncIO {
    import com.github.tototoshi.csv.defaultCSVFormat

    CSVReader.open(path.toFile).allWithHeaders().iterator.zipWithIndex.map { case (map, idx) =>
      def get[A](field: String)(parse: String => Either[String, A]) =
        map.get(field).toRight(s"missing field '$field'").flatMap(parse)
          .left.map(err => s"Row index $idx: $err").toValidatedNec

      (
        get("Device Name")(Right(_)),
        get("Condition Score")(OneToTenScore.parse),
        get("Battery Score")(Percentage.parse),
        get("Repair Score")(OneToTenScore.parse),
        get("Functionality Score")(OneToTenScore.parse),
        get("Appearance Score")(AppearanceScore.parse),
        get("History URL")(Right(_)),
        get("Left Barcode")(Right(_)),
        get("Right Barcode")(Right(_))
      ).mapN(LabelData.apply)
    }.toVector.sequence
  }
}
