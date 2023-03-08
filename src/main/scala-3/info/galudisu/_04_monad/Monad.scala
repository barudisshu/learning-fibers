package info.galudisu._04_monad

trait Monad[F[_]]:
  def pure[A](value: A): F[A]

  def flatMap[A, B](value: F[A])(func: A => F[B]): F[B]

  def map[A, B](value: F[A])(func: A => B): F[B] =
    flatMap(value)(a => pure(func(a)))

object Monad:
  def apply[F[_]](using monad: Monad[F]): Monad[F] = monad

extension[F[_] : Monad, A] (fa: F[A])

  /**
   * summon(召唤), 召唤一个 `given Monad[F]`, 通常该类型参数不是显式的。
   */
  def flatMap[B](f: A => F[B]): F[B] =
    summon[Monad[F]].flatMap(fa)(f)
