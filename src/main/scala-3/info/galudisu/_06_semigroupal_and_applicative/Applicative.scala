package info.galudisu._06_semigroupal_and_applicative

trait Apply[F[_]] extends Semigroupal[F] with Functor[F]:
  def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]

  def product[A, B](fa: F[A], fb: F[B]): F[(A, B)] =
    ap(map(fa)(a => (b: B) => (a, b)))(fb)

trait Applicative[F[_]] extends Apply[F]:
  def pure[A](a: A): F[A]
