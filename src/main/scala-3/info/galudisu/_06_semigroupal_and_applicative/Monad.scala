package info.galudisu._06_semigroupal_and_applicative

trait Monad[F[_]]:
  def map[A, B](fa: F[A])(func: A => B): F[B]

  def flatMap[A, B](fa: F[A])(func: A => F[B]): F[B]
