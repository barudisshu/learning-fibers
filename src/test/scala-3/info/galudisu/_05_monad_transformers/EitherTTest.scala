package info.galudisu._05_monad_transformers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.Await
import scala.concurrent.CanAwait
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.util.Try

class EitherTTest extends AnyWordSpec, Matchers, ScalaCheckPropertyChecks {

  "Transform and Roll Out" should {

    // type Response[A] = Future[Either[String, A]]
    type Response[A] = EitherT[Future, String, A]

    val powerLevels = Map(
      "Jazz" -> 6,
      "Bumblebee" -> 8,
      "Hot Rod" -> 10
    )

    given futMonad: Monad[Future] = new Monad[Future] {
      override def pure[A](value: A): Future[A] = Future.successful(value)

      override def flatMap[A, B](
                                  value: Future[A]
                                )(
                                  func: A => Future[B]): Future[B] =
        value.flatMap(func)
    }

    def getPowerLevel(autobot: String): Response[Int] =
      powerLevels.get(autobot) match {
        case Some(avg) => EitherT.right(Future(avg))
        case None => EitherT.left(Future(s"$autobot unreachable"))
      }

    "retrieve data from a set" in {
      getPowerLevel("Bumblebee").isRight === true
      getPowerLevel("Optimus Prime").isRight === false
    }

    def canSpecialMove(ally1: String, ally2: String): Response[Boolean] =
      for {
        power1 <- getPowerLevel(ally1)
        power2 <- getPowerLevel(ally2)
      } yield (power1 + power2) > 15

    "for-comprehension" in {
      canSpecialMove("Jazz", "Hot Rod") === true
      canSpecialMove("Jazz", "Bumblebee") === false
    }

    def tacticalReport(ally1: String, ally2: String): String = {
      val stack = canSpecialMove(ally1, ally2).run

      Await.result(stack, 1.seconds) match {
        case Left(msg) => s"Comms error: $msg"
        case Right(true) => s"$ally1 and $ally2 are ready to roll out!"
        case Right(false) => s"$ally1 and $ally2 need a rechange."
      }
    }

    "tractical report" in {
      tacticalReport("Jazz", "Bumblebee") shouldBe "Jazz and Bumblebee need a rechange."
      tacticalReport("Bumblebee", "Hot Rod") shouldBe "Bumblebee and Hot Rod are ready to roll out!"
      tacticalReport("Jazz", "Ironhide") contains "unreachable"
    }
  }
}
