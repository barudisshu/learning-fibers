package info.galudisu._03_functors

trait EndoFunctor[F[_]]:
  def map[A](fa: F[A])(f: A => A): F[A]
