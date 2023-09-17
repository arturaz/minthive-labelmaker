package minthive.labelmaker

import cats.data.Validated
import cats.effect.*
import cats.effect.std.Dispatcher
import cats.syntax.all.*
import com.itextpdf.kernel.pdf.{PdfDocument, PdfWriter}
import minthive.labelmaker.{AppearanceScore, LabelData, OneToTenScore, Percentage}

import java.io.File
import java.nio.file.Paths
import java.util.Locale

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    args match {
      case input :: output :: Nil =>
        val readInputsIO =
          SyncIO(Paths.get(input))
            .flatTap(path => SyncIO(println(s"Reading inputs from $path")))
            .flatMap(LabelDataCSVReader.readCSV)
            .to[IO]

        readInputsIO.flatMap {
          case Validated.Valid(labels) =>
            render(Paths.get(output).toFile, labels).as(ExitCode.Success)

          case Validated.Invalid(errors) =>
            IO {
              Console.err.println("Errors:")
              Console.err.println("")
              errors.iterator.foreach { error =>
                Console.err.println(error)
              }
              ExitCode.Error
            }
        }
      case _ =>
        IO(println("Usage: minthive-labelmaker <input.csv> <output.pdf>")).as(ExitCode.Error)
    }
  }

  def render(output: File, labels: Vector[LabelData]): IO[Unit] = {
    val resource = for {
      _ <- Resource.eval(IO(println(s"Writing output to $output")))
      pdfWriter <- Resource.make(IO(new PdfWriter(output)))(writer => IO(writer.close()))
      pdfDoc <- Resource.make(IO(new PdfDocument(pdfWriter)))(doc => IO(doc.close()))
      _ <- Resource.eval(IO(Locale.setDefault(Locale.US)))
      _ <- Resource.eval(LabelData.generate(labels, pdfDoc))
    } yield ()

    resource.use(_ => IO(println("Done.")))
  }
}