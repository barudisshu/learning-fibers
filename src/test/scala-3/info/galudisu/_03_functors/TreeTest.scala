package info.galudisu._03_functors

import info.galudisu._03_functors.Tree.*
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers

class TreeTest extends AnyWordSpec, Checkers {

  "Tree highkind-type" should {
    "mapping" in {
      check { (left: Int, right: Int) =>
        Tree.branch(Tree.leaf(left), Tree.leaf(right)).map(_ * 20) ==
          Tree.branch(
            Tree.leaf(left * 20),
            Tree.Leaf(right * 20)
          )
      }
    }
  }
}
