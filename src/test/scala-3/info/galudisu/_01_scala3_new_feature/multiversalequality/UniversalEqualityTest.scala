package info.galudisu._01_scala3_new_feature.multiversalequality

import info.galudisu._01_scala3_new_feature.multiversalequality.UniversalEquality.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UniversalEqualityTest extends AnyWordSpec, Matchers {
  "Universal equality check for different types" should {
    "compile without any errors and return false" in {
      "Square(5) == Circle(5)" should compile
      Square(5) should not be Circle(5)
    }
  }
}
