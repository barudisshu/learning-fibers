package info.galudisu._02_monoids_and_semigroups

import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import org.scalatest.wordspec.AnyWordSpec

class SemigroupTest extends Properties("Semigroup") {

  given semigroup: Semigroup[String] = new Semigroup[String] {
    override def combine(x: String, y: String): String = x.concat(y)
  }

  property("map") = forAll((x: String, y: String) => Semigroup[String].combine(x, y) == x.concat(y))
}
