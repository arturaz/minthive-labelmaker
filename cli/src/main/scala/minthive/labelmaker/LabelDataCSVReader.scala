package minthive.labelmaker

import com.github.tototoshi.csv.CSVReader
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.geom.{PageSize, Rectangle}
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.{PdfDocument, PdfPage}
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.{Paragraph, Text}
import com.itextpdf.layout.properties.{TextAlignment, VerticalAlignment}
import zio.prelude.*
import zio.{Task, ZIO}

import java.nio.file.Path

object LabelDataCSVReader {
  def readCSV(path: Path): Task[Validation[String, Vector[LabelData]]] = ZIO.attempt {
    import com.github.tototoshi.csv.defaultCSVFormat

    Validation.validateAll(
      CSVReader.open(path.toFile).allWithHeaders().iterator.zipWithIndex.map { case (map, idx) =>
        def get[A](field: String)(parse: String => Either[String, A]) =
          Validation.fromEither(
            map.get(field).toRight(s"missing field '$field'").flatMap(parse)
              .left.map(err => s"Row index $idx: $err")
          )
  
        Validation.validateWith(
          get("Device Name")(Right(_)),
          get("Condition Score")(OneToTenScore.parse),
          get("Battery Score")(Percentage.parse),
          get("Repair Score")(OneToTenScore.parse),
          get("Functionality Score")(OneToTenScore.parse),
          get("Appearance Score")(AppearanceScore.parse),
          get("History URL")(Right(_)),
          get("Left Barcode")(Right(_)),
          get("Right Barcode")(Right(_))
        )(LabelData.apply)
      }.toVector
    )
  }
}
