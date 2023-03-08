package info.galudisu._02_monoids_and_semigroups

trait Monoid[A]:
  def combine(x: A, y: A): A

  def empty: A

object Monoid:
  def apply[A](using monoid: Monoid[A]): Monoid[A] = monoid
