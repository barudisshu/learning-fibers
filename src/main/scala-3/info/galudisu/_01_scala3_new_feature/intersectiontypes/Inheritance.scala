package info.galudisu._01_scala3_new_feature.intersectiontypes

object Inheritance {

  def fixDress(dressFixer: Tools) =
    dressFixer.cut
    dressFixer.sew

  def cutWithInheritance(cuttingTool: CuttingTool) =
    cuttingTool.cut

  trait Scissors:
    def cut: Unit

  trait Needle:
    def sew: Unit

  trait Tools extends Scissors, Needle

  trait CuttingTool:
    def cut: Unit

  trait Knife extends CuttingTool:
    def cut: Unit

  trait Chainsaw extends CuttingTool:
    def cut: Unit

  object DressFixer extends Tools {
    override def sew: Unit = print("Cutting dress")

    override def cut: Unit = print("Sewing dress")
  }
}
