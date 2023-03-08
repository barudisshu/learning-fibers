package info.galudisu._01_scala3_new_feature.implicits

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.language.implicitConversions

class ImplicitConversionTest extends AnyWordSpec with Matchers {

  import ImplicitConversion.*

  "Given implicit conversion from String to Int, square" should {
    "square input type of string" in {
      given Conversion[String, Int] = Integer.parseInt(_)

      square("4") shouldBe 16
    }
  }
}
