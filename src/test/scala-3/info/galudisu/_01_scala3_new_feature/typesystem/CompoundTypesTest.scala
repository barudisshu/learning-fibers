package info.galudisu._01_scala3_new_feature.typesystem

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CompoundTypesTest extends AnyWordSpec, Matchers {

  import CompoundTypes.*
  import Intersection.*
  import Union.*

  "parse function which returns union type" should {
    "parse 123 to the integer type" in {
      parse("123") shouldBe 123
    }
    "return a string indicating that the input is not a number" in {
      parse("123Foo") shouldBe "Not a number"
    }
  }
  "shutdown function which require intersection of Show and Closable types" should {
    "shutdown a res object which is intersection of these two data types" in {
      shutdown(res) shouldBe()
    }
  }
}
