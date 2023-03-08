package info.galudisu._03_functors

trait Contravariant[F[_]]:
  def contramap[A, B](fa: F[A])(f: B => A): F[B]

object Contravariant:
  def apply[F[_]](using contravariant: Contravariant[F]): Contravariant[F] = contravariant
