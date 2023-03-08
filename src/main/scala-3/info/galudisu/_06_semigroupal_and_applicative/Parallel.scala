package info.galudisu._06_semigroupal_and_applicative

/**
 * [[FunctionK]] F -> G
 */
trait ~>[F[_], G[_]]:
  def apply[A](fa: F[A]): G[A]

trait Parallel[F[_]] {
  type G[_]

  def applicative: Applicative[G]

  def monad: Monad[F]

  def parallel: ~>[F, G]
}
