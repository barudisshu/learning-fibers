package info.galudisu._03_functors

object Tree:

  def branch[A](left: Tree[A], right: Tree[A]): Branch[A] = Branch(left, right)

  def leaf[A](value: A): Tree[A] = Leaf(value)

  given treeFunctor: Functor[Tree] = new Functor[Tree]:
    override def map[A, B](fa: Tree[A])(f: A => B): Tree[B] =
      fa match
        case Branch(left, right) =>
          Branch(map(left)(f), map(right)(f))
        case Leaf(value) => Leaf(f(value))

  sealed trait Tree[+A]

  final case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]

  final case class Leaf[A](value: A) extends Tree[A]

  extension[A, B] (tree: Tree[A])
    def map(f: A => B)(using treeFunctor: Functor[Tree]): Tree[B] = treeFunctor.map(tree)(f)
