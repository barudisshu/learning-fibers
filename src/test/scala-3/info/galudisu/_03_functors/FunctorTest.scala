package info.galudisu._03_functors

import org.scalacheck.Prop.*
import org.scalacheck.Properties
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class FunctorTest extends AnyWordSpec, Matchers, ScalaCheckDrivenPropertyChecks {

  "Functor Laws" should {

    given functorList: Functor[List] = new Functor[List] {
      override def map[A, B](fa: List[A])(f: A => B): List[B] = fa.map(f)
    }

    "identity raw" in {
      forAll((fa: List[Int]) => Functor[List].map(fa)(a => a) should equal(fa))
    }
    "composition raw" in {
      forAll {
        (
          fa: List[Int],
          g: Int => Int,
          f: Int => Int) =>
          Functor[List].map(fa)(a => g(f(a))) shouldBe Functor[List].map(fa)(f).map(g)
      }
    }
  }
}
