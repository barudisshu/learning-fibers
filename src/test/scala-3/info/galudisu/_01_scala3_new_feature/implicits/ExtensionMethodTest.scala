package info.galudisu._01_scala3_new_feature.implicits

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ExtensionMethodTest extends AnyWordSpec, Matchers {

  import ExtensionMethod.*

  "Using extension method for Int type" should {
    "provide that function as a method for Int values" in {
      5.square shouldBe 25
    }
  }
}
