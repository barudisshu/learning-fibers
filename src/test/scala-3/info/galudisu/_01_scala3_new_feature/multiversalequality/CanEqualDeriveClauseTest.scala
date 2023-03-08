package info.galudisu._01_scala3_new_feature.multiversalequality

import info.galudisu._01_scala3_new_feature.multiversalequality.CanEqualDeriveClause.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CanEqualDeriveClauseTest extends AnyWordSpec, Matchers {
  "CanEqualDeriveClause equality check for different types deriving CanEqual TypeClass" should {
    "compile successfully" in {
      "circle1 == circle2" should compile
      circle1 shouldEqual circle2
    }
  }
}
