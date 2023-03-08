package info.galudisu._06_semigroupal_and_applicative

/**
 * A type class that allows us to combine contexts.
 *
 * @tparam F
 * functor
 */
trait Semigroupal[F[_]]:
  def product[A, B](fa: F[A], fb: F[B]): F[(A, B)]

object Semigroupal:
  def apply[F[_]](using semigroupal: Semigroupal[F]): Semigroupal[F] =
    semigroupal
