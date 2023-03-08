package info.galudisu._02_monoids_and_semigroups

trait Semigroup[A]:
  def combine(x: A, y: A): A

object Semigroup:
  def apply[A](using semigroup: Semigroup[A]): Semigroup[A] = semigroup
