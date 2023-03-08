package info.galudisu._01_scala3_new_feature.intersectiontypes

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class OverloadingTest extends AnyWordSpec, Matchers {
  "Overloading" should {
    import Overloading.*
    "work as Intersection type alternative" in {
      cutPaper(Scissors()) shouldBe()
      cutPaper(Knife()) shouldBe()
    }
  }
}
