package info.galudisu._01_scala3_new_feature.intersectiontypes

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class BasicIntersectionTypeTest extends AnyWordSpec, Matchers {

  "Intersection Types" should {
    import BasicIntersectionType.*
    "check & is commutative" in {
      fixDressOne(DressFixer) shouldBe()
      fixDressTwo(DressFixer) shouldBe()
    }

    "use linearization to decide method override" in {
      generateNumbers(NumberGenerator21) shouldBe 2
      generateNumbers(NumberGenerator12) shouldBe 1
    }

    "make paper cutter work" in {
      cutPaper(PaperCutter) shouldBe()
    }
  }
}
