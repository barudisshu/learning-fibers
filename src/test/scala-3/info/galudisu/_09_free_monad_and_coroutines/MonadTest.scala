package info.galudisu._09_free_monad_and_coroutines;

import info.galudisu._09_free_monad_and_coroutines.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class MonadTest extends AnyWordSpec, Matchers, ScalaCheckPropertyChecks {

  given intMonad: Monad[List] = new Monad[List] {
    override def ap[A, B](ff: List[A => B])(value: List[A]): List[B] =
      ff.flatMap(f => value.map(f))

    override def pure[A](value: A): List[A] = List(value)

    override def flatMap[A, B](value: List[A])(func: A => List[B]): List[B] =
      value.flatMap(func)
  }

  "Monad Laws " should {
    "Left identity" in {
      def func(a: Int): List[String] = List(a.toString)

      forAll((a: Int) => Monad[List].pure(a).flatMap(func) == func(a))
    }
    lazy val m: Monad[Set] = new Monad[Set] {
      override def ap[A, B](ff: Set[A => B])(value: Set[A]): Set[B] =
        ff.flatMap(f => value.map(f))

      override def pure[A](value: A): Set[A] = Set(value)

      override def flatMap[A, B](value: Set[A])(func: A => Set[B]): Set[B] =
        value.flatMap(func)
    }
    "Right identity" in {
      forAll((pure: Set[Int]) => m.flatMap(pure) == m)
    }

    def f[A](a: A): Set[A] = Set(a)

    def g[A](b: A): Set[A] = Set(b)

    "Associativity" in {
      forAll((set: Set[Int]) => m.flatMap(set)(x => f(x)).flatMap(g) == m.flatMap(set)(x => f(x).flatMap(g)))
    }
  }

  "for-comprehension" should {
    "desugar " in {
      forAll { (a: List[Int], b: List[Int]) =>
        val result = for {
          x <- a
          y <- b
        } yield x + y
        val income = a.flatMap(x => b.map(y => x + y))
        result === income
      }
    }
  }
}
