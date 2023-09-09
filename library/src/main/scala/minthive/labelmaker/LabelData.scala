package minthive.labelmaker

import cats.effect.{IO, SyncIO}
import cats.syntax.all.*
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.geom.{PageSize, Rectangle}
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.{PdfDocument, PdfPage}
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.{Paragraph, Text}
import com.itextpdf.layout.properties.{TextAlignment, VerticalAlignment}


/** Data to be printed on the label. */
case class LabelData(
  deviceName: String,
  conditionScore: OneToTenScore,
  batteryScore: Percentage,
  repairScore: OneToTenScore,
  functionalityScore: OneToTenScore,
  appearanceScore: AppearanceScore,
  historyUrl: String,
  leftBarCode: String,
  rightBarCode: String,
) derives CanEqual {
  def render(doc: Document, page: PdfPage)(using fonts: Fonts): SyncIO[Unit] = {
    given Document = doc
    given PdfPage = page

    for {
      canvas <- SyncIO(new PdfCanvas(page))
      given PdfCanvas = canvas
      _ <- SyncIO(doc.showTextAligned(
        new Paragraph()
          .add(new Text(deviceName).setFont(fonts.bold).setFontSize(10))
          .setMultipliedLeading(1.4f)
          .setMaxWidth(Mm(55)),
        Mm(5), topY(Mm(5)), TextAlignment.LEFT, VerticalAlignment.TOP
      ))
      _ <- SyncIO(canvas.addImageFittedIntoRectangle(
        LabelData.ImageLogo,
        new Rectangle(rightX(Mm(5f + 36.8f)), topY(Mm(6f + 8.2f)), Mm(36.8), Mm(8.2)), false
      ))
      underDeviceName = topY(Mm(10))
      _ <- SyncIO(doc.showTextAligned(
        new Paragraph().add(new Text("Minthive pass").setFont(fonts.bold).setFontSize(22)),
        Mm(5), underDeviceName - Mm(12.2), TextAlignment.LEFT, VerticalAlignment.TOP
      ))
      h2FontSize = 14f
      _ <- SyncIO(doc.showTextAligned(
        new Paragraph().add(new Text("Overall device condition:").setFont(fonts.bold).setFontSize(h2FontSize)),
        Mm(5), underDeviceName - Mm(21), TextAlignment.LEFT, VerticalAlignment.TOP
      ))
      conditionScoreIndicatorY = underDeviceName - Mm(30)
      _ <- drawPercentageIndicator(
        rightX(Mm(20)), conditionScoreIndicatorY, radius = Mm(28.9f / 2), width = Mm(3.2), conditionScore.percentage,
        conditionScore.asString, belowText = None
      )
      _ <- drawDeviceConditionBar(
        Mm(5.4f + 4.1f), conditionScoreIndicatorY - Mm(2.5), buttonWidth = Mm(8.2), buttonHeight = Mm(3.9),
        borderWidth = Mm(0.4), conditionScore.roundedAsString
      )
      scoreBreakdownY = underDeviceName - Mm(50)
      _ <- SyncIO(doc.showTextAligned(
        new Paragraph().add(new Text("Condition score breakdown:").setFont(fonts.bold).setFontSize(h2FontSize)),
        Mm(5), scoreBreakdownY, TextAlignment.LEFT, VerticalAlignment.TOP
      ))
      scoreBreakdownIndicatorsDiameter = Mm(13.2f)
      scoreBreakdownIndicatorsRadius = scoreBreakdownIndicatorsDiameter / 2
      scoreBreakdownIndicatorsLeft = Mm(7.5f) + scoreBreakdownIndicatorsRadius
      scoreBreakdownIndicatorsSpacing = Mm(12.73)
      scoreBreakdownIndicatorsY = scoreBreakdownY - Mm(15)
      _ <- drawPercentageIndicator(
        scoreBreakdownIndicatorsLeft, scoreBreakdownIndicatorsY, radius = scoreBreakdownIndicatorsRadius,
        width = Mm(1.5), batteryScore,
        f"${(batteryScore.value * 100).round}%d%%", belowText = Some("Battery score")
      )
      _ <- drawPercentageIndicator(
        scoreBreakdownIndicatorsLeft + (scoreBreakdownIndicatorsDiameter + scoreBreakdownIndicatorsSpacing) * 1,
        scoreBreakdownIndicatorsY, radius = scoreBreakdownIndicatorsRadius, width = Mm(1.5),
        appearanceScore.toPercentage, appearanceScore.asString, belowText = Some("Appearance\nscore")
      )
      _ <- drawPercentageIndicator(
        scoreBreakdownIndicatorsLeft + (scoreBreakdownIndicatorsDiameter + scoreBreakdownIndicatorsSpacing) * 2,
        scoreBreakdownIndicatorsY, radius = scoreBreakdownIndicatorsRadius, width = Mm(1.5),
        repairScore.percentage, repairScore.asString, belowText = Some("Repair score")
      )
      _ <- drawPercentageIndicator(
        scoreBreakdownIndicatorsLeft + (scoreBreakdownIndicatorsDiameter + scoreBreakdownIndicatorsSpacing) * 3,
        scoreBreakdownIndicatorsY, radius = scoreBreakdownIndicatorsRadius, width = Mm(1.5),
        functionalityScore.percentage, functionalityScore.asString, belowText = Some("Functionality\nscore")
      )
      evaluationLabelY = scoreBreakdownIndicatorsY - Mm(18)
      _ <- SyncIO(doc.showTextAligned(
        new Paragraph().add(new Text("Minthive evaluation").setFont(fonts.bold).setFontSize(h2FontSize)),
        Mm(5), evaluationLabelY, TextAlignment.LEFT, VerticalAlignment.TOP
      ))
      textFontSize = 8f
      _ <- SyncIO {
        def text(str: String, font: PdfFont = fonts.normal) =
          new Text(str).setFont(font).setFontSize(textFontSize)

        doc.showTextAligned(
          new Paragraph()
            .add(text("All devices with "))
            .add(text("Minthive Label", fonts.bold))
            .add(text(" were\nevaluated by professionals and score is\ngenerated based on "))
            .add(text("device battery").setUnderline())
            .add(text(" and \n"))
            .add(text("functionality").setUnderline())
            .add(text(" statuses, "))
            .add(text("appearance").setUnderline())
            .add(text("\nand "))
            .add(text("repair history").setUnderline())
            .add(text("."))
            .setMultipliedLeading(1.2f)
          ,
          Mm(5), evaluationLabelY - Mm(7), TextAlignment.LEFT, VerticalAlignment.TOP
        )
      }
      qrCodeSize = Mm(24)
      qrCodeCenterX = rightX(qrCodeSize)
      qrCodeCenterY = evaluationLabelY + Mm(1.5f) - qrCodeSize / 2
      _ <- addQrCode(historyUrl, qrCodeCenterX, qrCodeCenterY, qrCodeSize, qrCodeSize)
      _ <- SyncIO(doc.showTextAligned(
        new Paragraph("CHECK HISTORY").setFont(fonts.bold).setFontSize(10),
        qrCodeCenterX, qrCodeCenterY - qrCodeSize / 2, TextAlignment.CENTER, VerticalAlignment.TOP
      ))
      barCodeY = Mm(8f + 16.8f / 2)
      barCodeHeight = Mm(14.8)
      _ <- addBarCode(leftBarCode, Mm(5f + 41.8f / 2), barCodeY, Mm(41.8), barCodeHeight)
      _ <- addBarCode(rightBarCode, rightX(Mm(5f + 45.4f / 2)), barCodeY, Mm(45.4), barCodeHeight)
      barCodeLineY = barCodeY + barCodeHeight / 2 + Mm(2.5)
      _ <- drawSingleLine(Mm(5), barCodeLineY, rightX(Mm(5)), barCodeLineY, lineWidth = Mm(0.4))

      pageCenterX = page.getPageSize.getWidth / 2
      recyclingSymbolWidth = Mm(6.1)
      weeeSymbolWidth = Mm(5.7)
      weeeSymbolY = Mm(1.9)
      smallerSymbolY = Mm(4)
      symbolSpacing = Mm(1)
      _ <- SyncIO(canvas.addImageFittedIntoRectangle(
        LabelData.ImageRecyclingSymbol,
        new Rectangle(
          pageCenterX - weeeSymbolWidth / 2 - recyclingSymbolWidth - symbolSpacing, smallerSymbolY,
          recyclingSymbolWidth, Mm(5.7)
        ), false
      ))
      _ <- SyncIO(canvas.addImageFittedIntoRectangle(
        LabelData.ImageWeeeSymbol,
        new Rectangle(pageCenterX - weeeSymbolWidth / 2, weeeSymbolY, weeeSymbolWidth, Mm(7.9)), false
      ))
      _ <- SyncIO(canvas.addImageFittedIntoRectangle(
        LabelData.ImageCeSymbol,
        new Rectangle(pageCenterX + weeeSymbolWidth / 2 + symbolSpacing, smallerSymbolY, Mm(7.5), Mm(5.7)), false
      ))
    } yield ()
  }
}
object LabelData {
  val ImageLogo = ImageDataFactory.createPng(getClass.getResourceAsStream("/images/logo.png").readAllBytes)
  val ImageCeSymbol = ImageDataFactory.createPng(getClass.getResourceAsStream("/images/symbol-ce.png").readAllBytes)
  val ImageRecyclingSymbol = ImageDataFactory.createPng(getClass.getResourceAsStream("/images/symbol-recycling.png").readAllBytes)
  val ImageWeeeSymbol = ImageDataFactory.createPng(getClass.getResourceAsStream("/images/symbol-weee.png").readAllBytes)

  /**
   * @note This returns [[IO]] instead of [[SyncIO]] because big document generations take a lot of time and then we
   *       hit fairness issues. See [[https://typelevel.org/cats-effect/docs/thread-model#fibers]] for more information.
   */
  def generate(datas: Vector[LabelData], pdfDoc: PdfDocument): IO[Unit] = {
    for {
      doc <- IO {
        val doc = new Document(pdfDoc, PageSize.A6)
        val docInfo = pdfDoc.getDocumentInfo
        docInfo.setCreator("Minthive Label Generator by Artūras Šlajus <as@arturaz.net>")
        docInfo.setProducer("https://github.com/arturaz/minthive-labelmaker")
        doc
      }
      fonts <- Fonts.create.to[IO]
      _ <- fonts.addToDoc(pdfDoc).to[IO]
      _ <- datas.map { data =>
        IO(pdfDoc.addNewPage()).flatMap(data.render(doc, _)(using fonts).to[IO])
      }.sequence_
    } yield ()
  }
}