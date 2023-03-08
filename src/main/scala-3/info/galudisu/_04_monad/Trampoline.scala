package info.galudisu._04_monad

/**
 * Making every call a self recursive tail call
 *
 * @tparam A
 */
sealed trait Trampoline[+A]:
  def resume: Either[() => Trampoline[A], A] = this match {
    case Done(v) => Right(v)
    case More(k) => Left(k)
    case FlatMap(sub, cont) =>
      sub match {
        case Done(v) => cont(v).resume
        case More(k) => Left(() => FlatMap(k(), cont))
        case FlatMap(sub2, cont2) =>
          sub2.flatMap(x => cont2(x).flatMap(cont)).resume
      }
  }

  def flatMap[B](f: A => Trampoline[B]): Trampoline[B] = More[B](() => f(runT))

  @scala.annotation.tailrec
  final def runT: A = resume match {
    case Right(value) => value
    case Left(more) => more().runT
  }

final case class Done[A](value: A) extends Trampoline[A]

final case class More[A](call: () => Trampoline[A]) extends Trampoline[A]

final case class FlatMap[A, +B](sub: Trampoline[A], cont: A => Trampoline[B]) extends Trampoline[B]
