package info.galudisu._01_scala3_new_feature.intersectiontypes

object BasicIntersectionType {

  def fixDressOne(dressFixer: Scissors & Needle) =
    dressFixer.cut
    dressFixer.sew

  def fixDressTwo(dressFixer: Needle & Scissors) =
    dressFixer.cut
    dressFixer.sew

  def cutPaper(pc: Knife & Scissors) =
    pc.cut

  def generateNumbers(generator: OneGenerator & TwoGenerator) =
    generator.generate

  trait Scissors:
    def cut: Unit

  trait Needle:
    def sew: Unit

  trait Knife:
    def cut: Unit

  trait Chainsaw:
    def cut: Unit

  trait OneGenerator:
    def generate: Int = 1

  trait TwoGenerator:
    def generate: Int = 2

  object DressFixer extends Scissors, Needle {
    override def cut: Unit = print("Cutting dress")

    override def sew: Unit = print("Sewing dress")
  }

  object PaperCutter extends Knife, Scissors {
    override def cut: Unit = print("Cutting stuff")
  }

  object NumberGenerator21 extends OneGenerator, TwoGenerator {
    override def generate: Int = super.generate
  }

  object NumberGenerator12 extends TwoGenerator, OneGenerator {
    override def generate: Int = super.generate
  }
}
