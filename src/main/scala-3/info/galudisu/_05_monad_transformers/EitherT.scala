package info.galudisu._05_monad_transformers

import info.galudisu._03_functors.Functor

import scala.concurrent.Future

trait Monad[F[+_]]:
  def pure[A](value: A): F[A]

  def flatMap[A, B](value: F[A])(func: A => F[B]): F[B]

  def map[A, B](value: F[A])(func: A => B): F[B] =
    flatMap(value)(a => pure(func(a)))

object Monad:
  def apply[F[+_]](using monad: Monad[F]): _root_.info.galudisu._05_monad_transformers.Monad[F] = monad

type M[F[+_]] = Monad[F]

/**
 * [[EitherT]] is the [[Either]] transformer that received the Monad stack by the parameter [[run]] wich [[F]] presented
 * the Functor.
 */
final case class EitherT[F[+_] : M, A, +B](run: F[Either[A, B]]):
  def pure[AA >: A, D](value: AA): EitherT[F, AA, D] = EitherT(pureF(value))

  def pureF[AA >: A, D](value: AA): F[Either[AA, D]] =
    Monad[F].pure(Left[AA, D](value))

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

  def isLeft: F[Boolean] = Monad[F].map(run)(_.isLeft)

  def isRight: F[Boolean] = Monad[F].map(run)(_.isRight)

object EitherT:
  def left[F[+_], A, B](fa: F[A])(using monad: Monad[F]): EitherT[F, A, B] =
    EitherT(monad.map(fa)(Left(_)))

  def right[F[+_], A, B](fa: F[B])(using monad: Monad[F]): EitherT[F, A, B] =
    EitherT(monad.map(fa)(Right(_)))
