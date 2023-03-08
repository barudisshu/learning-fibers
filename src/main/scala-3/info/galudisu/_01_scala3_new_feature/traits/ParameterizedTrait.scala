package info.galudisu._01_scala3_new_feature.traits

object ParameterizedTrait {

  trait Base(val msg: String)

  class Foo extends Base("Foo")

  class Bar extends Base("Bar")
}
