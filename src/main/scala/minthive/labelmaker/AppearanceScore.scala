package minthive.labelmaker

import utils.*

enum AppearanceScore {
  case A, `B+`, B, `C+`, C

  def toPercentage: Percentage = (this match {
    case A => Percentage(1)
    case `B+` => Percentage(0.9)
    case B => Percentage(0.8)
    case `C+` => Percentage(0.7)
    case C => Percentage(0.6)
  }).get

  def asString: String = this match {
    case A => "A"
    case `B+` => "B+"
    case B => "B"
    case `C+` => "C+"
    case C => "C"
  }
}
object AppearanceScore {
  def parse(s: String): Either[String, AppearanceScore] = s match {
    case "A" => Right(A)
    case "B+" => Right(`B+`)
    case "B" => Right(B)
    case "C+" => Right(`C+`)
    case "C" => Right(C)
    case _ => Left(s"Invalid appearance score: $s")
  }
}