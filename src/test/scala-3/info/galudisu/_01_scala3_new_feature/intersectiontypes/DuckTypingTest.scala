package info.galudisu._01_scala3_new_feature.intersectiontypes

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DuckTypingTest extends AnyWordSpec, Matchers {
  "DuckTyping" should {
    import DuckTyping.*
    "work as Intersection type alternative" in {
      // Scala 3 not recommend to using `new` keyword and will be remove in the future
      // cutPaper(new Scissors()) shouldBe ()
      cutPaper(Scissors()) shouldBe()
      cutPaper(Knife()) shouldBe()
    }
  }
}
