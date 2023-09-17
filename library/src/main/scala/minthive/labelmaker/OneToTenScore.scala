package minthive.labelmaker

import minthive.labelmaker.utils.*

/** Range: 1-10, inclusive. */
case class OneToTenScore private (score: Double) extends AnyVal derives CanEqual {
  def percentage: Percentage = Percentage(score / 10).get

  /** Rounds the value to the same representation as [[asString]] uses. */
  def roundedAsString: OneToTenScore = new OneToTenScore(f"$score%.1f".toDouble)

  def asString: String  = if (score == 10) "10" else f"$score%.1f"
}
object OneToTenScore {
  given Conversion[OneToTenScore, Double] = _.score

  def apply(score: Double): Either[String, OneToTenScore] = {
    if (score >= 1 && score <= 10) Right(new OneToTenScore(score))
    else Left(s"Score must be between 1 and 10, inclusive. Got $score")
  }

  def parse(s: String): Either[String, OneToTenScore] = {
    val trimmed = s.trim
    trimmed.toDoubleOption.toRight(s"Could not parse '$trimmed' as a score [1..10]").flatMap(apply)
  }
}
