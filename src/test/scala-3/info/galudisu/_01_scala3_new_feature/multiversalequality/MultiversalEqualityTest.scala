package info.galudisu._01_scala3_new_feature.multiversalequality

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MultiversalEqualityTest extends AnyWordSpec, Matchers {
  "Multiversal equality check for different types" should {
    " throw Type error and not compile" in {
      import scala.language.strictEquality
      import MultiversalEquality.*
      assertTypeError("fido == rover")
      "fido == rover" shouldNot compile
    }
  }
}
