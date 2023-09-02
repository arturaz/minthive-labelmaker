package minthive.labelmaker

case class Mm(mm: Float) extends AnyVal derives CanEqual {
  def +(other: Mm): Mm = Mm(mm + other.mm)
  def -(other: Mm): Mm = Mm(mm - other.mm)
  def *(multiplier: Float): Mm = Mm(mm * multiplier)
  def /(divisor: Float): Mm = Mm(mm / divisor)
}
object Mm {
  final val MmToPixels = 2.83501683501683501684f

  /** Converts from millimeters to pixels. */
  given Conversion[Mm, Float] = _.mm * MmToPixels

  def fromPixels(pixels: Float): Mm = Mm(pixels / MmToPixels)
}