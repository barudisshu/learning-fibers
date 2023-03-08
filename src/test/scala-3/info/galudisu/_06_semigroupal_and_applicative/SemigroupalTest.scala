package info.galudisu._06_semigroupal_and_applicative

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SemigroupalTest extends AnyWordSpec, Matchers {

  "Semigroupal is a type class that allows us to combine contexts" should {

    // for-comprehension
    given Semigroupal[Option] with
      override def product[A, B](fa: Option[A], fb: Option[B]): Option[(A, B)] =
        for {
          a <- fa
          b <- fb
        } yield (a, b)
    "allows us to join contexts" in {
      Semigroupal[Option].product(Some(123), Some("abc")) shouldBe Some(
        (123, "abc")
      )
    }

    // 不推荐的用法，Scala3将会移除 tuple2 to tuple22 contexts的实现
    // https://dotty.epfl.ch/docs/reference/dropped-features/limit22.html
    extension[A, B, C] (semigroupal: Semigroupal.type)
      def tuple3(
                  fa: Option[A],
                  fb: Option[B],
                  fc: Option[C]): Option[Tuple3[A, B, C]] =
        for {
          a <- fa
          b <- fb
          c <- fc
        } yield (a, b, c)

    "joining three or more contexts" in {
      Semigroupal.tuple3(Option(1), Option(2), Option(3)) shouldBe Some(
        (1, 2, 3)
      )
    }

    extension[A] (semigroupal: Semigroupal.type)
      def map3(
                fa: Option[A],
                fb: Option[A],
                fc: Option[A]
              )(
                func: (A, A, A) => A): Option[A] =
        fa.flatMap(a => fb.flatMap(b => fc.map(c => func(a, b, c))))

    "map/contramap/imap more contexts" in {
      Semigroupal.map3(Option(1), Option(2), Option(3))(_ + _ + _)
    }
  }
}
