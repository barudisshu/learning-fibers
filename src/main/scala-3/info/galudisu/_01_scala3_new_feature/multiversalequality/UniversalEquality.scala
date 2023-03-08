package info.galudisu._01_scala3_new_feature.multiversalequality

object UniversalEquality extends App {

  val square = Square(5)
  val circle = Circle(5)

  case class Square(length: Float)

  case class Circle(radius: Float)

  println(square == circle) // prints false, No compilation errors

}
