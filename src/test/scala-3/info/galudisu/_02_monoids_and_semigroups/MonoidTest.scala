package info.galudisu._02_monoids_and_semigroups

import org.scalacheck.Arbitrary.*
import org.scalacheck.Prop.*
import org.scalacheck.Properties
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MonoidTest extends Properties("Monoid") {

  given monoid: Monoid[Int] = new Monoid[Int] {
    override def combine(x: Int, y: Int): Int = x + y

    override def empty: Int = 0
  }

  def associativeLow[A](
                         x: A,
                         y: A,
                         z: A
                       )(using m: Monoid[A]): Boolean =
    m.combine(x, m.combine(y, z)) == m.combine(m.combine(x, y), z)

  property("associativeLaw") = forAll(
    (
      x: Int,
      y: Int,
      z: Int) => associativeLow(x, y, z) == true)

  def identityLaw[A](x: A)(using m: Monoid[A]): Boolean =
    (m.combine(x, m.empty) == x) && (m.combine(m.empty, x) == x)

  property("identityLaw") = forAll((x: Int) => identityLaw(x))

  property("map") = forAll { (x: Int, y: Int) =>
    Monoid[Int].combine(x, y) == x + y
    Monoid.apply[Int].combine(x, y) == x + y
  }

  property("unit") = forAll { (x: Int) =>
    Monoid[Int].empty == 0
    Monoid.apply[Int].combine(x, Monoid[Int].empty) == x
  }
}
