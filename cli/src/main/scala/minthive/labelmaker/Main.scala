package minthive.labelmaker

import com.itextpdf.kernel.pdf.{PdfDocument, PdfWriter}
import minthive.labelmaker.{AppearanceScore, LabelData, OneToTenScore, Percentage}
import zio.{Scope, ZIO, ZIOApp, ZIOAppArgs, ZIOAppDefault}
import zio.prelude.Validation

import java.io.File
import java.nio.file.Paths
import java.util.Locale

object Main extends ZIOAppDefault {
  override def run = ZIO.serviceWithZIO[ZIOAppArgs] { args =>
    args.getArgs.toList match {
      case input :: output :: Nil =>
        val readInputsIO =
          ZIO.attempt(Paths.get(input).toAbsolutePath)
            .tap(path => ZIO.attempt(println(s"Reading inputs from $path")))
            .flatMap(LabelDataCSVReader.readCSV)

        readInputsIO.flatMap {
          case Validation.Success(_, labels) =>
            render(Paths.get(output).toFile, labels)

          case Validation.Failure(_, errors) =>
            ZIO.attempt {
              Console.err.println("Errors:")
              Console.err.println("")
              errors.iterator.foreach { error =>
                Console.err.println(error)
              }
              sys.exit(1)
            }
        }
      case _ =>
        ZIO.attempt(println("Usage: minthive-labelmaker <input.csv> <output.pdf>")) *> ZIO.attempt(sys.exit(1))
    }
  }

  def render(output: File, labels: Vector[LabelData]): ZIO[Any, Throwable, Unit] = {
    ZIO.scoped(for {
      _ <- ZIO.attempt(println(s"Writing output to $output"))
      pdfWriter <- ZIO.acquireRelease(ZIO.attempt(new PdfWriter(output)))(writer => ZIO.succeed(writer.close()))
      pdfDoc <- ZIO.acquireRelease(ZIO.attempt(new PdfDocument(pdfWriter)))(doc => ZIO.succeed(doc.close()))
      _ <- ZIO.attempt(Locale.setDefault(Locale.US))
      _ <- LabelData.generate(labels, pdfDoc)
      _ <- ZIO.attempt(println("Done."))
    } yield ())
  }
}