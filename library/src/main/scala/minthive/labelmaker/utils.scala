package minthive.labelmaker

import com.itextpdf.barcodes.{Barcode128, BarcodeQRCode}
import com.itextpdf.kernel.colors.{Color, DeviceRgb}
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.{PdfDocument, PdfPage}
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.{Image, Paragraph}
import com.itextpdf.layout.properties.{TextAlignment, VerticalAlignment}
import zio.{Task, ZIO}

import scala.annotation.targetName

def rightX(x: Float)(using page: PdfPage): Float = page.getPageSize.getWidth - x
@targetName("rightXMm")
def rightX(x: Mm)(using page: PdfPage): Mm = Mm.fromPixels(page.getPageSize.getWidth) - x

def topY(y: Float)(using page: PdfPage): Float = page.getPageSize.getHeight - y
@targetName("topYMm")
def topY(y: Mm)(using page: PdfPage): Mm = Mm.fromPixels(page.getPageSize.getHeight) - y

def circlePoint(centerX: Float, centerY: Float, radius: Float, angleRad: Double): (Float, Float) =
  (
    centerX + radius * math.cos(angleRad).toFloat,
    centerY - radius * math.sin(angleRad).toFloat
  )


def circlePoints(
  centerX: Float, centerY: Float, radius: Float, width: Float,
  fromPercent: Percentage, toPercent: Percentage
): IndexedSeq[(Float, Float)] = {
  val pointCount = 100

  val piOffset = -math.Pi / 2
  val start = math.Pi * 2.0 * fromPercent + piOffset
  val end = math.Pi * 2.0 * toPercent + piOffset
  val range = end - start
  val step = range / pointCount

  // Outer ring
  val outerRing = (0 to pointCount).map { idx =>
    val angle = if (idx == pointCount - 1) end else start + idx * step
    circlePoint(centerX, centerY, radius, angle)
  }

  val innerRadius = radius - width
  val lineTowardsInnerRing = Vector(circlePoint(centerX, centerY, innerRadius, end))

  // Inner ring
  val innerRing = (0 to pointCount).reverse.map { idx =>
    val angle = if (idx == 0) start else start + idx * step
    circlePoint(centerX, centerY, innerRadius, angle)
  }

  val lineTowardsOuterRing = Vector(circlePoint(centerX, centerY, radius, start))

  outerRing ++ lineTowardsInnerRing ++ innerRing ++ lineTowardsOuterRing
}

def drawCircle(
  points: Iterable[(Float, Float)], fillColor: Color
)(using canvas: PdfCanvas): Task[Unit] = ZIO.attempt {
  canvas.setLineWidth(0)
  canvas.setFillColor(fillColor)
  canvas.moveTo(points.head._1, points.head._2)
  points.iterator.drop(1).foreach { case (x, y) => canvas.lineTo(x, y) }
  canvas.fill()
  canvas.setFillColor(Black)
  canvas.setLineWidth(1)
  ()
}

def drawPercentageIndicator(
  centerX: Float, centerY: Float, radius: Float, width: Float, percentage: Percentage, text: String,
  belowText: Option[String]
)(using doc: Document, canvas: PdfCanvas, fonts: Fonts): Task[Unit] = {
  for {
    _ <- drawCircle(circlePoints(centerX, centerY, radius, width, Percentage._0, percentage), Black)
    threshold = 0.99
    _ <-
      if (percentage < threshold) drawCircle(
        circlePoints(centerX, centerY, radius, width, percentage + Percentage._0_05, Percentage._99_5), Grey
      )
      else ZIO.unit
    _ <- ZIO.attempt(doc.showTextAligned(
      new Paragraph(text).setFontSize(radius * 0.6f).setFont(fonts.bold),
      centerX, centerY, TextAlignment.CENTER, VerticalAlignment.MIDDLE
    ))
    _ <- belowText.fold(ZIO.unit) { text =>
      ZIO.attempt(doc.showTextAligned(
        new Paragraph(text).setFontSize(radius * 0.4f).setFont(fonts.normal),
        centerX, centerY - radius * 1.3f, TextAlignment.CENTER, VerticalAlignment.TOP
      ))
    }
  } yield ()
}

def roundedRectanglePoints(
  centerX: Float, centerY: Float, width: Float, height: Float
): IndexedSeq[(Float, Float)] = {
  val halfHeight = height / 2
  val notRoundedWidth = width - height

  val circlePointCount = 50
  val circleCenterLeft = centerX - notRoundedWidth / 2
  val circleCenterRight = centerX + notRoundedWidth / 2
  val circleCenterY = centerY

  val piOffset = -math.Pi / 2

  val circleLeftStart = math.Pi + piOffset // Start at the bottom
  val circleLeftEnd = piOffset // Finish at the top
  val circleLeftRange = circleLeftStart - circleLeftEnd
  val circleLeftStep = circleLeftRange / circlePointCount

  val circleRightStart = piOffset // Start at the top
  val circleRightEnd = math.Pi + piOffset // Finish at the bottom
  val circleRightRange = circleRightEnd - circleRightStart
  val circleRightStep = circleRightRange / circlePointCount

  val rightCirclePoints = (0 to circlePointCount).map { idx =>
    val angle = if (idx == circlePointCount - 1) circleRightEnd else circleRightStart + idx * circleRightStep
    circlePoint(circleCenterRight, circleCenterY, halfHeight, angle)
  }
  val bottomLinePoints = Vector((circleCenterLeft, centerY - halfHeight))
  val leftCirclePoints = (0 to circlePointCount).map { idx =>
    val angle = if (idx == 0) circleLeftStart else circleLeftStart + idx * circleLeftStep
    circlePoint(circleCenterLeft, circleCenterY, halfHeight, angle)
  }
  val topLinePoints = Vector((circleCenterRight, centerY + halfHeight))

  rightCirclePoints ++ bottomLinePoints ++ leftCirclePoints ++ topLinePoints
}

def roundedRectanglePoints(
  centerX: Float, centerY: Float, width: Float, height: Float, borderWidth: Float
): IndexedSeq[(Float, Float)] = {
  val outerRectanglePoints = roundedRectanglePoints(centerX, centerY, width, height)
  val innerRectanglePoints =
    roundedRectanglePoints(centerX, centerY, width - borderWidth * 2, height - borderWidth * 2)

  outerRectanglePoints ++ innerRectanglePoints.reverse :+ outerRectanglePoints.head
}

def roundedRectanglePoints(
  centerX: Float, centerY: Float, width: Float, height: Float, borderWidth: Option[Float]
): IndexedSeq[(Float, Float)] =
  borderWidth match {
    case Some(borderWidth) => roundedRectanglePoints(centerX, centerY, width, height, borderWidth)
    case None => roundedRectanglePoints(centerX, centerY, width, height)
  }

def drawSingleLine(
  fromX: Float, fromY: Float, toX: Float, toY: Float, lineWidth: Float
)(using canvas: PdfCanvas): Task[Unit] =
  for {
    _ <- ZIO.attempt(canvas.setLineWidth(lineWidth))
    _ <- ZIO.attempt(canvas.moveTo(fromX, fromY))
    _ <- ZIO.attempt(canvas.lineTo(toX, toY))
    _ <- ZIO.attempt(canvas.fillStroke())
  } yield ()

def drawDeviceConditionBar(
  startX: Float, startY: Float, buttonWidth: Float, buttonHeight: Float, borderWidth: Float,
  deviceCondition: OneToTenScore
)(using doc: Document, fonts: Fonts, canvas: PdfCanvas): Task[Unit] = {
  def drawButton(
    scoreText: String, selected: Boolean, x: Float, y: Float
  ): Task[Unit] = {
    for {
      _ <- drawCircle(
        roundedRectanglePoints(x, y, buttonWidth, buttonHeight, if (selected) None else Some(borderWidth)),
        Black
      )
      _ <- ZIO.attempt(canvas.setFillColor(if (selected) White else Black))
      _ <- ZIO.attempt(doc.showTextAligned(
        new Paragraph(scoreText).setFontSize(buttonHeight * 0.6f).setFont(fonts.bold),
        x, y, TextAlignment.CENTER, VerticalAlignment.MIDDLE
      ))
      _ <- ZIO.attempt(canvas.setFillColor(Black))
    } yield ()
  }

  val spacing = Mm(0.4)
  def xForIdx(idx: Int): Float = startX + idx * (buttonWidth + spacing)
  for {
    _ <- drawButton("5", deviceCondition < 6, xForIdx(0), startY)
    _ <- drawButton("6", deviceCondition >= 6 && deviceCondition < 7, xForIdx(1), startY)
    _ <- drawButton("7", deviceCondition >= 7 && deviceCondition < 8, xForIdx(2), startY)
    _ <- drawButton("8", deviceCondition >= 8 && deviceCondition < 9, xForIdx(3), startY)
    _ <- drawButton("9", deviceCondition >= 9 && deviceCondition < 10, xForIdx(4), startY)
    endX = xForIdx(5)
    _ <- drawButton("10", deviceCondition >= 10, endX, startY)
    lineSpacing = Mm(0.2)
    lineWidth = Mm(0.4)
    lineLength = Mm(5.4)
    lineTextSize = buttonHeight * 0.7f
    lineStartY = startY - buttonHeight / 2 - lineSpacing
    lineEndY = startY - buttonHeight / 2 - lineSpacing - lineLength
    _ <- drawSingleLine(startX, lineStartY, startX, lineEndY, lineWidth = lineWidth)
    _ <- ZIO.attempt(doc.showTextAligned(
      new Paragraph("Broken\ndevice").setFontSize(lineTextSize).setFont(fonts.normal),
      startX, lineEndY - lineSpacing, TextAlignment.CENTER, VerticalAlignment.TOP
    ))
    _ <- drawSingleLine(endX, lineStartY, endX, lineEndY, lineWidth = lineWidth)
    _ <- ZIO.attempt(doc.showTextAligned(
      new Paragraph("New\ndevice").setFontSize(lineTextSize).setFont(fonts.normal),
      endX, lineEndY - lineSpacing, TextAlignment.CENTER, VerticalAlignment.TOP
    ))
  } yield ()
}

def addQrCode(
  contents: String, centerX: Float, centerY: Float, width: Float, height: Float
)(using doc: Document, canvas: PdfCanvas): Task[Unit] = ZIO.attempt {
  val rectangle = new Rectangle(centerX - width / 2, centerY - height / 2, width, height)
  val qrCode = new BarcodeQRCode(contents)
  val formXObject = qrCode.createFormXObject(Black, doc.getPdfDocument)
  canvas.addXObjectFittedIntoRectangle(formXObject, rectangle)
}

def addBarCode(
  contents: String, centerX: Float, centerY: Float, width: Float, height: Float
)(using doc: Document, canvas: PdfCanvas, fonts: Fonts): Task[Unit] = ZIO.attempt {
  val rectangle = new Rectangle(centerX - width / 2, centerY - height / 2, width, height)
  val barCode = new Barcode128(doc.getPdfDocument)
  barCode.setCode(contents)
  barCode.fitWidth(width)
  barCode.setBarHeight(height * 0.8f)
  barCode.setFont(fonts.normal)
  val formXObject = barCode.createFormXObject(Black, Black, doc.getPdfDocument)
  canvas.addXObjectFittedIntoRectangle(formXObject, rectangle)
}