package info.galudisu._09_free_monad_and_coroutines

/**
 * ![stack safe](http://blog.higher-order.com/assets/trampolines.pdf)
 *
 * @tparam F
 * @tparam G
 */
trait ~>[F[_], G[_]]:
  def apply[A](fa: F[A]): G[A]

enum Free[F[_], A]:
  case Pure(a: A) extends Free[F, A]
  case Suspend(a: F[A]) extends Free[F, A]
  case FlatMapped[G[_], In, Out](self: Free[G, In], f: In => Free[G, Out]) extends Free[G, Out]

  final def flatMap[B](f: A => Free[F, B]): Free[F, B] =
    FlatMapped[F, A, B](this, f)

  final def map[B](f: A => B): Free[F, B] =
    FlatMapped[F, A, B](this, a => Free.pure(f(a)))

  final def foldMap[G[_] : Monad](using nt: F ~> G): G[A] = this match {
    case Pure(value) => Monad[G].pure(value)
    case Suspend(fa) => nt(fa)
    case FlatMapped(inner, f) =>
      val ge = inner.foldMap(Monad[G], nt)
      Monad[G].flatMap(ge)(in => f(in).foldMap(Monad[G], nt))
  }

object Free:
  def pure[F[_], A](a: A): Free[F, A] = Free.Pure(a)

  def liftM[F[_], A](fa: F[A]): Free[F, A] = Free.Suspend(fa)
