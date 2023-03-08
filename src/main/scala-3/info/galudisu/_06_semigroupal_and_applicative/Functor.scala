package info.galudisu._06_semigroupal_and_applicative

trait Functor[F[_]]:
  def map[A, B](value: F[A])(func: A => B): F[B]

object Functor:
  def apply[F[_]](using functor: Functor[F]) = functor
