package info.galudisu._09_free_monad_and_coroutines

trait Functor[F[_]]:
  def map[A, B](fa: F[A])(func: A => B): F[B]

trait Semigroupal[F[_]]:
  def product[A, B](fa: F[A], fb: F[B]): F[(A, B)]

trait Apply[F[_]] extends Semigroupal[F], Functor[F]:
  def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]

  override def product[A, B](fa: F[A], fb: F[B]): F[(A, B)] =
    ap(map(fa)(a => (b: B) => (a, b)))(fb)

trait Applicative[F[_]] extends Apply[F]:
  def pure[A](a: A): F[A]

trait FlatMap[F[_]] extends Apply[F]:
  def flatMap[A, B](fa: F[A])(func: A => F[B]): F[B]

/**
 * Monad.
 *
 * Allows composition of dependent effectful functions.
 *
 * @see
 * [[http://homepages.inf.ed.ac.uk/wadler/papers/marktoberdorf/baastad.pdf Monads for functional programming]]
 *
 * Must obey the laws defined in cats.laws.MonadLaws.
 */
trait Monad[F[_]] extends Applicative[F], FlatMap[F]:
  override def map[A, B](fa: F[A])(func: A => B): F[B] =
    flatMap(fa)(a => pure(func(a)))

object Monad:
  def apply[F[_]](using monad: Monad[F]): Monad[F] = monad
