package minthive.labelmaker

import cats.data.Validated
import cats.effect.*
import cats.syntax.all.*
import com.itextpdf.kernel.pdf.{PdfDocument, PdfWriter}
import minthive.labelmaker.{AppearanceScore, LabelData, OneToTenScore, Percentage}
import utils.*

import java.nio.file.Paths
import java.util.Locale

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    args match {
      case input :: output :: Nil =>
        val readInputsIO =
          SyncIO(Paths.get(input))
            .flatTap(path => SyncIO(println(s"Reading inputs from $path")))
            .flatMap(LabelData.readCSV)

        readInputsIO.flatMap {
          case Validated.Valid(labels) =>
            render(output, labels).as(ExitCode.Success)

          case Validated.Invalid(errors) =>
            SyncIO {
              Console.err.println("Errors:")
              Console.err.println("")
              errors.iterator.foreach { error =>
                Console.err.println(error)
              }
              ExitCode.Error
            }
        }.to[IO]
      case _ =>
        IO(println("Usage: minthive-labelmaker <input.csv> <output.pdf>")).as(ExitCode.Error)
    }
  }

  def render(output: String, labels: Vector[LabelData]): SyncIO[Unit] = {
    val resource = for {
      _ <- Resource.eval(SyncIO(println(s"Writing output to $output")))
      pdfWriter <- Resource.make(SyncIO(new PdfWriter(output)))(writer => SyncIO(writer.close()))
      pdfDoc <- Resource.make(SyncIO(new PdfDocument(pdfWriter)))(doc => SyncIO(doc.close()))
      _ <- Resource.eval(SyncIO(Locale.setDefault(Locale.US)))
      _ <- Resource.eval(LabelData.generate(labels, pdfDoc))
    } yield ()

    resource.use(_ => SyncIO(println("Done.")))
  }
}