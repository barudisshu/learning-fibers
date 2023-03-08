package info.galudisu._04_monad

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class FreeTest extends AnyWordSpec, Matchers, ScalaCheckPropertyChecks {

  "tail recursive optimization" should {
    def fib(num: Int): BigInt = {
      @scala.annotation.tailrec
      def _fib(
                n: Int,
                acc1: BigInt,
                acc2: BigInt): BigInt = n match {
        case 0 => acc1
        case 1 => acc2
        case _ => _fib(n - 1, acc2, acc1 + acc2)
      }

      _fib(num, 0, 1)
    }

    "accomplished " in {
      fib(6) shouldBe 8
    }
  }

  "lazy implementing " should {
    def fib: LazyList[BigInt] = 0 #:: fib.scan(BigInt(1))(_ + _)

    "scala stream" in {
      fib(6) shouldBe 8
    }
  }

  "tail call elimination problem" should {
    def even[A](ls: List[A]): Boolean =
      ls match {
        case Nil => true
        case x :: xs => odd(xs)
      }

    def odd[A](ls: List[A]): Boolean =
      ls match {
        case Nil => false
        case x :: xs => even(xs)
      }

    "prone" in {
      // blows the stack
      // even((0 to 1_000_000).toList)
      true
    }
  }

  "scala tail call optimization" should {
    import scala.util.control.TailCalls.*

    def fac(n: Int): TailRec[Int] =
      if n == 0 then done(1)
      else
        for {
          x <- tailcall(fac(n - 1))
        } yield n * x

    "runloop" in {
      fac(3).result shouldBe 6
    }
  }

  "trampoline of tail call" should {
    def even[A](ls: Seq[A]): Trampoline[Boolean] =
      ls match {
        case Nil => Done(true)
        case x :: xs => More(() => odd(xs))
      }

    def odd[A](ls: Seq[A]): Trampoline[Boolean] =
      ls match {
        case Nil => Done(false)
        case x :: xs => More(() => even(xs))
      }

    // 多个函数的尾调用问题，转换为单一一个`Trampoline`的尾递归优化问题
    @scala.annotation.tailrec
    def run[A](trampoline: Trampoline[A]): A =
      trampoline match {
        case Done(v) => v
        case More(t) => run(t())
        case FlatMap(sub, cont) => ???
      }

    extension[A] (trampoline: Trampoline[A]) def runM = run(trampoline)
    "reach done" in {
      even((0 to 1_000_000).toList).runM shouldBe false
      even((0 to 1_000_000).toList).runT shouldBe false
    }
  }

  "state monad stack overflows" should {
    case class State[S, +A](runS: S => (S, A)) {
      def map[B](f: A => B): State[S, B] =
        State[S, B] { s =>
          val (s1, a) = runS(s)
          (s1, f(a))
        }

      def flatMap[B](f: A => State[S, B]): State[S, B] =
        State[S, B] { s =>
          val (s1, a) = runS(s)
          f(a).runS(s1)
        }
    }

    def getState[S]: State[S, S] = State(s => (s, s))

    def setState[S](s: S): State[S, Unit] = State(_ => (s, ()))

    def pureState[S, A](a: A): State[S, A] = State(s => (s, a))

    def zipIndex[A](as: Seq[A]): State[Int, List[(Int, A)]] =
      as.foldLeft(pureState[Int, List[(Int, A)]](List()))((acc, a) =>
        for {
          xs <- acc
          n <- getState
          _ <- setState(n + 1)
        } yield (n, a) :: xs)

    "prone" in {
      // blows the stack
      // zipIndex(0 to 1_000_000).runS(0)
      true
    }
  }

  "State monad's flatMap operate " should {

    case class State[S, +A](runS: S => Trampoline[(S, A)]) {
      def map[B](f: A => B): State[S, B] = State[S, B](
        runS.andThen { tramp =>
          val (s, a) = tramp.runT
          Done((s, f(a)))
        }
      )

      def flatMap[B](f: A => State[S, B]): State[S, B] = State[S, B](
        runS.andThen(tramp => FlatMap[(S, A), (S, B)](tramp, { case (s, a) => f(a).runS(s) }))
      )
    }

    "suspend " in {
      true
    }
  }

  "FlatMap State monad " should {
    case class State[S, +A](runS: S => Trampoline[(S, A)]) {
      def map[B](f: A => B): State[S, B] = State[S, B](
        runS.andThen { tramp =>
          val (s, a) = tramp.runT
          Done((s, f(a)))
        }
      )

      def flatMap[B](f: A => State[S, B]): State[S, B] = State[S, B](
        runS.andThen(tramp => tramp.flatMap { case (s, a) => f(a).runS(s) })
      )
    }

    "suspend " in {
      true
    }
  }

}
