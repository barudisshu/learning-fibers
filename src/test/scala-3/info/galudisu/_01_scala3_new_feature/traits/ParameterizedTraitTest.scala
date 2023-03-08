package info.galudisu._01_scala3_new_feature.traits

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ParameterizedTraitTest extends AnyWordSpec, Matchers {

  import ParameterizedTrait.*

  "Traits can have parameters" in {
    val foo = Foo()
    assert(foo.msg == "Foo")
  }
}
