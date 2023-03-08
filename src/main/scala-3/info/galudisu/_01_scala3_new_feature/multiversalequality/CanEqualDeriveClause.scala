package info.galudisu._01_scala3_new_feature.multiversalequality

import scala.language.strictEquality

object CanEqualDeriveClause extends App {
  val circle1 = Circle(5)
  val circle2 = Circle(5)

  case class Circle(radius: Float)derives CanEqual
  println(circle1 == circle2) // No compilation errors & prints true.
}
