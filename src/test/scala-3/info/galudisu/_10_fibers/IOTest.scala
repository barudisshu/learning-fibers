package info.galudisu._10_fibers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.annotation.tailrec

class IOTest extends AnyWordSpec, Matchers, ScalaCheckPropertyChecks {

  "Free Monad is tail recursive optimization" should {

    sealed trait Trampoline[A]

    case class Tell(statement: String) extends Trampoline[Unit]
    case class Ask(question: String) extends Trampoline[String]

    def tell(statement: String): Free[Trampoline, Unit] = Free.liftM(Tell(statement))

    def ask(question: String): Free[Trampoline, String] = Free.liftM(Ask(question))

    type Id[A] = A

    // Execution can be impure, side effects are OK here
    given(Trampoline ~> Id) with
      override def apply[A](ui: Trampoline[A]): Id[A] = ui match {
        case Tell(statement) => println(statement)
        case Ask(question) =>
          // here come side effect
          println(question)
          val sideEffect = "<referentially transparent>"
          sideEffect.asInstanceOf[A]
      }

    given monadId: Monad[Id] = new Monad[Id] {
      override def tailRecM[A, B](a: A)(func: A => Id[Either[A, B]]): Id[B] = func(a) match {
        case Right(r) => r
        case Left(l) => tailRecM(l)(func)
      }

      override def ap[A, B](ff: Id[A => B])(fa: Id[A]): Id[B] = ff(fa)

      override def pure[A](value: A): Id[A] = value

      override def flatMap[A, B](fa: Id[A])(func: A => Id[B]): Id[B] = func(fa)
    }

    "protected by a trampoline of tail call" in {
      val program: Free[Trampoline, Unit] = for {
        _ <- tell("Hello!")
        name <- ask("What is your name?")
        _ <- tell(s"Hi, $name")
      } yield ()

      val eval: Unit = program.foldMap[Id]
      true
    }
  }

  "IO Monad is just embbed the side effect" should {

    case class NotAInt(str: String) extends Throwable

    def sum(i: IO[Int], j: IO[Int]): IO[Int] = {
      for {
        a <- i
        b <- j
      } yield a + b
    }

    "no need to carry out the monad" in {
      forAll { (x: Int, y: Int) =>
        val result: IO[Int] = sum(IO.effect(x), IO.effect(y))
        result.unsafeRunSync() shouldBe (x + y)
      }
    }

    "unsafe run" in {
      List
        .fill(10_000)(1)
        .foldRight(IO.effect(0))((i, s) => s.map(_ + i))
        .unsafeRunAsync(_.fold(_ => println("fail"), i => println(s"succ: $i")))
      true
    }

    def getUserIdByEmail(str: String): IO[Long] =
      if !str.contains("@") then IO.raiseError(Exception("Invalid Email"))
      else IO.pure(1L)

    def getUsersCosts(id: Long): IO[Array[Int]] =
      if id == 1 then IO.pure(Array[Int](1, 2, 3))
      else IO.raiseError(Exception("There are no costs"))

    def getReport(costs: Array[Int]): IO[String] = IO.pure("Mega report")

    "real world business" in {
      val email = "SomeEmail"

      val program = for {
        id <- getUserIdByEmail(email)
        costs <- getUsersCosts(id)
        report <- getReport(costs)
      } yield report

      program.unsafeRunAsync(_.fold(_ => println("fail"), _ => println("succ")))
      true
    }
  }
}
