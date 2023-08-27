package utils

extension [A, B](e: Either[A, B]) {
  def get: B = e match {
    case Right(b) => b
    case Left(a) => throw new NoSuchElementException(s"Expected Right, got Left($a)")
  }
}