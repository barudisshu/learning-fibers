package info.galudisu._01_scala3_new_feature.intersectiontypes

object Overloading {

  def cutPaper(cutter: Knife) = println("Cutting with Knife")

  def cutPaper(cutter: Scissors) = println("Cutting with Scissors")

  class Knife()

  class Scissors()

}
