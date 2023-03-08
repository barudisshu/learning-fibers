package info.galudisu._01_scala3_new_feature.multiversalequality

import scala.language.strictEquality

object MultiversalEquality extends App {

  val rover = Dog("Rover")
  val fido = Dog("Fido")

  case class Dog(name: String)

  // fido == rover  // Throws compile error : "Values of types Dog and Dog cannot be compared with == or !="

}
