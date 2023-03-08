package info.galudisu._04_monad

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.Future

class MonadTest extends AnyWordSpec, Matchers, ScalaCheckPropertyChecks {

  given intMonad: Monad[List] = new Monad[List] {
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

  given opt: Monad[Option] = new Monad[Option] {
    override def pure[A](value: A): Option[A] = Option(value)

    override def flatMap[A, B](
                                value: Option[A]
                              )(
                                func: A => Option[B]): Option[B] =
      value.flatMap(func)
  }

  "Monad Usage" should {
    def g: Int => Option[Int] = a => Some(a + 2)

    def f: Int => Int = a => 100 * a

    "option" in {
      forAll { (a: Int, b: Option[Int]) =>
        Monad[Option].pure(a) == Option(a)
        Monad[Option].flatMap(b)(g) == b.flatMap(g)
        Monad[Option].map(b)(f) == b.map(f)
      }
    }
    "list" in {
      def g: Int => List[Int] = a => List(a, a * 10)

      def f: Int => Int = a => a + 123

      forAll { (a: List[Int]) =>
        Monad[List].flatMap(a)(g) == a.flatMap(g)
        Monad[List].map(a)(f) == a.map(f)
      }
    }
    "future" in {
      import scala.concurrent.ExecutionContext.Implicits.global
      given futImpl: Monad[Future] = new Monad[Future] {
        override def pure[A](value: A): Future[A] = Future.successful(value)

        override def flatMap[A, B](
                                    value: Future[A]
                                  )(
                                    func: A => Future[B]): Future[B] =
          value.flatMap(func)
      }

      def g: Int => Future[Int] = a => Future.successful(a)

      def f: Int => Int = a => a + 1314

      forAll((fut: Future[Int]) => Monad[Future].flatMap(fut)(g) == fut.flatMap(g))
    }
  }
}
