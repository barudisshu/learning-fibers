package info.galudisu._01_scala3_new_feature.infixmethods

import scala.annotation.showAsInfix

object InfixMethods {

  // permissive with method names, ::, ++, -->
  // infix methods

  val person = Person("Mary")

  def main(args: Array[String]): Unit =
    println(person.enjoys2("rock"))
  person.likes("Forrest Gump")
  person.likes("Forrest Gump") // identical -=- for methods with OME arg

  // extension method
  extension (person: Person)
    @showAsInfix
    def enjoys2(musicGenre: String): String =
      s"${person.name} listens to $musicGenre"

  case class Person(name: String):
    @showAsInfix // only required for alphanumeric names
    def likes(movie: String): String = s"$name likes $movie"
}
