package info.galudisu._01_scala3_new_feature.implicits

object WritingTypeclassInstances {

  trait Ord[T]:
    def compare(x: T, y: T): Int

  given Ord[Int] with
    override def compare(x: Int, y: Int): Int =
      if x < y then -1 else if x > y then +1 else 0
}
