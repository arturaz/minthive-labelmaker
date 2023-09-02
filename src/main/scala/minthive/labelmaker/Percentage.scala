package minthive.labelmaker

import utils.*

/** Range: 0 to 1, inclusive. */
case class Percentage private (value: Double) extends AnyVal derives CanEqual {
  def +(other: Percentage): Percentage = Percentage(value + other.value).get
}
object Percentage {
  val _0: Percentage = new Percentage(0)
  val _0_05: Percentage = new Percentage(0.005)
  val _99_5: Percentage = new Percentage(0.995)
  val _100: Percentage = new Percentage(1)

  def apply(value: Double): Either[String, Percentage] =
    if (value >= 0 && value <= 1) Right(new Percentage(value))
    else Left(s"Percentage must be between 0 and 1, inclusive. Got $value")

  def parse(s: String): Either[String, Percentage] = {
    val trimmed = s.trim
    trimmed.toDoubleOption.toRight(s"Could not parse '$trimmed' as a percentage [0..1]").flatMap(apply)
  }

  given Conversion[Percentage, Double] = _.value
}
