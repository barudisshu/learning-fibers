package info.galudisu._01_scala3_new_feature.multiversalequality

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CanEqualGivenInstanceTest extends AnyWordSpec, Matchers {
  "CanEqualGivenInstance equality check for different types" should {
    " compile and return true" in {
      import scala.language.strictEquality
      import CanEqualGivenInstance.*
      "email == letter" should compile
      email shouldEqual letter
    }
  }
}
