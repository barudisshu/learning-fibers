package info.galudisu._10_fibers

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.control.NonFatal

trait Semigroup[A]:
  def combine(x: A, y: A): A

trait Monoid[A] extends Semigroup[A]:
  def empty: A

trait Functor[F[_]]:
  def map[A, B](fa: F[A])(func: A => B): F[B]

trait Semigroupal[F[_]]:
  def product[A, B](fa: F[A])(fb: F[B]): F[(A, B)]

trait Apply[F[_]] extends Functor[F], Semigroupal[F]:
  def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]

  def mapN[A, B, C](fa: F[A], fb: F[B])(ff: (A, B) => C): F[C] =
    map(product(fa)(fb)) {
      case (a, b) => ff(a, b)
    }

  override def product[A, B](fa: F[A])(fb: F[B]): F[(A, B)] =
    ap(map(fa)((a: A) => (b: B) => (a, b)))(fb)

trait Applicative[F[_]] extends Apply[F]:
  def pure[A](a: A): F[A]

  override def map[A, B](fa: F[A])(func: A => B): F[B] = ap(pure(func))(fa)

trait FlatMap[F[_]] extends Apply[F]:
  def flatMap[A, B](fa: F[A])(func: A => F[B]): F[B]

  def flatten[A](ffa: F[F[A]]): F[A] = flatMap(ffa)(fa => fa)

  /**
   * Keeps calling `f` until a `scala.util.Right[B]` is returned.
   *
   * Based on Phil Freeman's [[http://functorial.com/stack-safety-for-free/index.pdf Stack Safety for Free]].
   *
   * Implementations of this method should use constant stack space relative to `f`.
   */
  def tailRecM[A, B](a: A)(func: A => F[Either[A, B]]): F[B]

/**
 * Weak Monad, you should impl [[tailRecM]] since it's not stack safe.
 *
 * @tparam F
 */
trait Monad[F[_]] extends FlatMap[F], Applicative[F]:
  override def map[A, B](fa: F[A])(func: A => B): F[B] = flatMap(fa)(a => pure(func(a)))

object Monad:
  def apply[F[_]](using monad: Monad[F]): Monad[F] = monad

type M[F[+_]] = Monad[F]

final case class EitherT[F[+_] : M, A, +B](val run: F[Either[A, B]]):
  def pure[AA >: A, D](value: AA): EitherT[F, AA, D] = EitherT(pureF(value))

  def map[D](f: B => D): EitherT[F, A, D] = mapEither(_.map(f))

  def mapEither[D](f: Either[A, B] => Either[A, D]): EitherT[F, A, D] = EitherT(
    mapEitherF(f)
  )

  def mapEitherF[D](f: Either[A, B] => Either[A, D]): F[Either[A, D]] =
    Monad[F].map(run)(f)

  /**
   * It's left flatMap, but the behaviours same with rightFlatMap
   */
  def flatMap[AA >: A, D](f: B => EitherT[F, AA, D]): EitherT[F, AA, D] =
    flatMapF(k => f(k).run)

  def flatMapF[AA >: A, D](f: B => F[Either[AA, D]]): EitherT[F, AA, D] =
    EitherT(flatMapEitherF(f))

  def flatMapEitherF[AA >: A, D](f: B => F[Either[AA, D]]): F[Either[AA, D]] =
    Monad[F].flatMap(run)(k => k.fold[F[Either[AA, D]]](pureF, f))

  def pureF[AA >: A, D](value: AA): F[Either[AA, D]] =
    Monad[F].pure(Left[AA, D](value))

  def isLeft: F[Boolean] = Monad[F].map(run)(_.isLeft)

  def isRight: F[Boolean] = Monad[F].map(run)(_.isRight)

object EitherT:
  def left[F[+_], A, B](fa: F[A])(using monad: Monad[F]): EitherT[F, A, B] =
    EitherT(monad.map(fa)(Left(_)))

  def right[F[+_], A, B](fa: F[B])(using monad: Monad[F]): EitherT[F, A, B] =
    EitherT(monad.map(fa)(Right(_)))

/**
 * FunctionK
 *
 * @tparam F
 * @tparam G
 */
trait ~>[F[_], G[_]]:
  def apply[A](fa: F[A]): G[A]

/**
 * Stack safe Monad
 */
enum Free[F[_], A]:
  case Pure(a: A) extends Free[F, A]
  case Suspend(fa: F[A]) extends Free[F, A]
  case FlatMapped[G[_], In, Out](self: Free[G, In], func: In => Free[G, Out]) extends Free[G, Out]

  final def map[B](func: A => B): Free[F, B] = FlatMapped[F, A, B](this, a => Free.Pure(func(a)))

  final def flatMap[B](func: A => Free[F, B]): Free[F, B] = FlatMapped[F, A, B](this, func)

  /**
   * Evaluate a single layer of the free monad.
   *
   * @param functor
   * @return
   */
  @tailrec
  final def resume(using functor: Functor[F]): Either[F[Free[F, A]], A] = this match {
    case Pure(value) => Right(value)
    case Suspend(fa) => Left(functor.map(fa)(Pure(_)))
    case FlatMapped(inner, f) =>
      inner match {
        case Pure(a) => f(a).resume
        case Suspend(fa) => Left(functor.map(fa)(f))
        case FlatMapped(next, g) => next.flatMap(a => g(a).flatMap(f)).resume
      }
  }

  /**
   * Catamorphism for `Free`.
   *
   * Run to completion, mapping the suspension with the given transformation at each step and accumulating into the
   * monad `M`.
   *
   * This method uses `tailRecM` to provide stack-safety.
   */
  final def foldMap[G[_] : Monad](using nt: F ~> G): G[A] =
    Monad[G].tailRecM(this)(_.step match {
      case Pure(a) => Monad[G].pure(Right(a))
      case Suspend(fa) => Monad[G].map(nt(fa))(Right(_))
      case FlatMapped(inner, f) => Monad[G].map(inner.foldMap(Monad[G], nt))(next => Left(f(next)))
    })

  @tailrec
  final def step: Free[F, A] =
    this match {
      case FlatMapped(FlatMapped(c, f), g) => c.flatMap(cc => f(cc).flatMap(g)).step
      case FlatMapped(Pure(a), f) => f(a).step
      case x => x
    }

object Free:
  def pure[F[_], A](a: A): Free[F, A] = Pure(a)

  def liftM[F[_], A](fa: F[A]): Free[F, A] = Suspend(fa)

/**
 * Simple IO, embbedding all possible side effects thought was never absolutely needed.
 *
 *   - FF: wrap side-effects into `IO`
 *   - Combinators: build complex `IO`s by composing smaller ones
 *   - Runners: translate `IO` to side-effects at the end of the world
 *
 * Base on [[https://github.com/SystemFw/Scala-World-2019/blob/master/fibers.md]]
 */
enum IO[+A]:
  case Pure(v: A) extends IO[A]
  case Effect(eff: () => A) extends IO[A]
  case FlatMapped[In, Out](self: IO[In], f: In => IO[Out]) extends IO[Out]
  case Failure(e: Throwable) extends IO[A]
  case Recover(self: IO[A], handler: Throwable => IO[A]) extends IO[A]
  case AsyncF(k: (Either[Throwable, A] => Unit) => IO[Unit]) extends IO[A]

  // it's a monad
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

    def loop(current: IO[Any], stack: List[Bind]): A = current match {
      case FlatMapped(io, k) => loop(io, stack.push(Bind.K(k.asInstanceOf[Any => IO[Any]])))
      case Recover(io, h) => loop(io, stack.push(Bind.H(h)))
      case Effect(body) =>
        try loop(Pure(body()), stack)
        catch {
          case NonFatal(e) => loop(Failure(e), stack)
        }
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
      case AsyncF(_) => loop(Failure(IllegalArgumentException("cannot handle async")), stack)
    }

    loop(this, List.empty[Bind])
  }

  final def unsafeRunAsync(cb: Either[Throwable, A] => Unit): Unit = {
    def loop(
              current: IO[Any],
              stack: List[Any => IO[Any]],
              cb: Either[Throwable, A] => Unit): Unit =
      current match {
        case FlatMapped(io, k) => loop(io, stack.push(k.asInstanceOf[Any => IO[Any]]), cb)
        case Recover(io, h) => ()
        case Effect(body) =>
          try loop(Pure(body()), stack, cb)
          catch {
            case NonFatal(e) => loop(Failure(e), stack, cb)
          }
        case Pure(v) =>
          stack.pop match {
            case None => cb(Right(v.asInstanceOf[A]))
            case Some((bind, stack)) =>
              val nextIO = bind(v)
              loop(nextIO, stack, cb)
          }
        case Failure(e) => cb(Left(e))
        case AsyncF(asyncProcess) =>
          val restOfComputation = { (res: Either[Throwable, Any]) =>
            val nextIO = res.fold(Failure(_), Pure(_))
            loop(nextIO, stack, cb)
          }
          asyncProcess(restOfComputation)
      }

    loop(this, List.empty, cb)
  }

object IO:
  def pure[A](a: A): IO[A] = IO.Pure(a)

  def effect[A](body: => A): IO[A] = IO.Effect(() => body)

  def raiseError[A](e: Throwable): IO[A] = IO.Failure(e)

  def recover[A](fa: IO[A])(handler: Throwable => IO[A]): IO[A] = IO.Recover(fa, handler)

object Console:
  def getStr(): IO[String] = IO.effect[String](scala.io.StdIn.readLine)

  def putStrLn(str: String): IO[Unit] = IO.effect(println(str))
