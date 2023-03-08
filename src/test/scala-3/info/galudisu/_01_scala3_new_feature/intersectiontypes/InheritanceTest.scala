package info.galudisu._01_scala3_new_feature.intersectiontypes

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InheritanceTest extends AnyWordSpec, Matchers {
  "Inheritance" should {
    import Inheritance.*
    "work as Intersection type alternative" in {
      fixDress(DressFixer) shouldBe()
    }
  }
}
