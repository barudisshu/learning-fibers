package info.galudisu._01_scala3_new_feature.intersectiontypes

import scala.reflect.Selectable.reflectiveSelectable

object DuckTyping {

  type ToolToCutPaper = {
    val canCutPaper: Boolean
    def cut(): Unit
  }

  def cutPaper(pc: ToolToCutPaper) = pc.cut()

  class Scissors() {
    val canCutPaper: Boolean = true

    def cut(): Unit = println("Cutting with Scissors")
  }

  class Knife() {
    val canCutPaper: Boolean = true

    def cut(): Unit = println("Cutting with Knife")
  }

  class Chainsaw() {
    def cut(): Unit = println("Cutting with Chainsaw")
  }

}
