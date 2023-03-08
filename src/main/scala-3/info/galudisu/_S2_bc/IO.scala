package info.galudisu._S2_bc

import org.bouncycastle.util.io.pem.PemObject

import java.io.File
import scala.annotation.tailrec
import scala.util.control.NonFatal

enum IO[+A]:
  case Pure(v: A) extends IO[A]
  case Effect(eff: () => A) extends IO[A]
  case FlatMapped[In, Out](self: IO[In], f: In => IO[Out]) extends IO[Out]
  case Failure(e: Throwable) extends IO[A]
  case Recover(self: IO[A], handler: Throwable => IO[A]) extends IO[A]

  final def flatMap[B](f: A => IO[B]): IO[B] = FlatMapped[A, B](this, f)

  final def map[B](f: A => B): IO[B] = FlatMapped[A, B](this, a => IO.pure(f(a)))

  extension[T] (list: List[T])
    def push(a: => T): List[T] = a :: list
    def pop: Option[(T, List[T])] =
      if list.isEmpty then Option.empty[(T, List[T])]
      else Option(list.head, list.tail)

  final def unsafeRunSync(): A = {
    sealed trait Bind {
      def isHandler: Boolean = this.isInstanceOf[Bind.H]
    }
    object Bind {
      case class K(f: Any => IO[Any]) extends Bind

      case class H(f: Throwable => IO[Any]) extends Bind
    }

    @tailrec
    def loop(current: IO[Any], stack: List[Bind]): A = current match {
      case FlatMapped(io, k) => loop(io, stack.push(Bind.K(k.asInstanceOf[Any => IO[Any]])))
      case Recover(io, h) => loop(io, stack.push(Bind.H(h)))
      case Effect(body) => loop(Pure(body()), stack)
      case Pure(v) =>
        stack.dropWhile(_.isHandler) match {
          case Nil => v.asInstanceOf[A]
          case Bind.K(f) :: stack => loop(f(v), stack)
          case _ => loop(Failure(RuntimeException("wont happen")), stack)
        }
      case Failure(e) =>
        stack.dropWhile(!_.isHandler) match {
          case Nil => throw e
          case Bind.H(f) :: stack => loop(f(e), stack)
          case _ => loop(Failure(RuntimeException("wont happen")), stack)
        }
    }

    loop(this, List.empty[Bind])
  }

object IO:
  def pure[A](a: A): IO[A] = IO.Pure(a)

  def effect[A](body: => A): IO[A] = IO.Effect(() => body)

  def raiseError[A](e: Throwable): IO[A] = IO.Failure(e)

  def recover[A](fa: IO[A])(handler: Throwable => IO[A]): IO[A] = IO.Recover(fa, handler)

object PemReader:
  def read(read: => PemObject): IO[PemObject] = IO.effect[PemObject](read)
