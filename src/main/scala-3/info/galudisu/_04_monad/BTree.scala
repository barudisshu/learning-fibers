package info.galudisu._04_monad

enum BTree[+T]:
  case Leaf[A](v: A) extends BTree[A]
  case Branch[A](left: BTree[A], right: BTree[A]) extends BTree[A]
