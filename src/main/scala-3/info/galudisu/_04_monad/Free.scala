package info.galudisu._04_monad

trait ~>[F[_], G[_]]:
  def apply[A](fa: F[A]): G[A]

/**
 * The core idea of Free is to switch from effectful functions (that might be impure) to plain data structures that
 * represents our domain logic. Those data structures are composed in a structure that is free to interpretation and the
 * exact implementation of our program can be decided later.
 */
enum Free[F[_], A]:
  case Pure(value: A) extends Free[F, A]
  case Suspend(fa: F[A]) extends Free[F, A]
  case FlatMap[G[_], In, Out](self: Free[G, In], f: In => Free[G, Out]) extends Free[G, Out]

  final def flatMap[B](f: A => Free[F, B]): Free[F, B] = FlatMap[F, A, B](this, f)

  final def map[B](f: A => B): Free[F, B] = FlatMap[F, A, B](this, a => Free.pure(f(a)))

  final def foldMap[G[_] : Monad](using nt: F ~> G): G[A] =
    this match
      case Pure(value) => Monad[G].pure(value)
      case Suspend(fa) => nt(fa)
      case FlatMap(inner, f) =>
        val ge = inner.foldMap(Monad[G], nt)
        Monad[G].flatMap(ge)(in => f(in).foldMap(Monad[G], nt))

object Free:
  def pure[F[_], A](a: A): Free[F, A] = Free.Pure(a)

  def liftM[F[_], A](fa: F[A]): Free[F, A] = Free.Suspend(fa)
