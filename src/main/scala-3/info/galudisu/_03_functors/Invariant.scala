package info.galudisu._03_functors

trait Invariant[F[_]]:
  def imap[A, B](fa: F[A])(f: A => B)(g: B => A): F[B]

object Invariant:
  def apply[F[_]](using invariant: Invariant[F]) = invariant
