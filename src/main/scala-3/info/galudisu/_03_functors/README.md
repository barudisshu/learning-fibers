# Functors

函子

## Functor

函子的定义属于高阶函数实现，

- 映射：`(A => B) => F[B]`

> **Functor Laws**
>
> Functors guarantee the same semantics whether we sequence many small operations one by one, or
> combine them into a larger function before mapping. To ensure this is the case the following laws
> must hold:
>
> _identity_ : calling map with the identity function is the same as doing nothing:
>
> ```scala
> fa.map(a => a) == fa
> ```
>
> _Composition_ : mapping with two functions `f` and `g` is the same as mapping with `f` and then
> mapping with `g`:
>
> ```scala
> fa.map(a => g(f(a))) == fa.map(f).map(g)
> ```
>

### Exercise: Branching out with Functors

Write a Functor for the following binary tree data type. Verify that the code
works as expected on instances of Branch and Leaf:

```scala
sealed trait Tree[+A]
final case class Branch[A](left: Tree[A], right: Tree[A])
extends Tree[A]
final case class Leaf[A](value: A) extends Tree[A]
```

## Contravariant Functors and the contramap Method

逆变(contravariant)函子

### Exercise: Showing off with Contramap

Implement the contramap method for Printable above. Start with the following code template and
replace the ??? with a working method body:

```scala
trait Printable[A] {
  def format(value: A): String
  def contramap[B](func: B => A): Printable[B] =
    new Printable[B] {
      def format(value: B): String =
        ???
    }
}
```

If you get stuck, think about the types. You need to turn value, which is of type B, into a String.
What functions and methods do you have available and in what order do they need to be combined?

Now define an instance of Printable for the following Box case class. You'll need to write this as
an `using` def method:

```scala
final case class Box[A](value: A)
```

Your instance should work as follows:

```scala
format(Box("hello world"))
// res4: String = "'hello world'"
format(Box(true))
// res5: String = "yes"
```

## Invariant Functors and the imap Method

协变(Invariant)函子

> **What's With the Names?**
>
> What's the relationship between the terms "contravariance", "invariance", and "covariance" and
> these difference kinds of functor?
>
> If you recall from Section 1.6.1, variance affects subtyping, which is essentially our ability to
> use a value of one type in place of a value of another type without breaking the code.
>
> Subtyping can be viewed as a conversion. If `B` is a subtype of `A`, we can always convert a `B`
> to an `A`.
>
> Equivalently we could say that `B` is a subtype of `A` if there exists a function `B => A`. `A`
> standard covariant functor (协变函子) captures exactly this. If `F` is a covariant functor, wherever
> we have an `F[B]` and a conversion `B => A` we can always convert to an `F[A]`.
>
> A contravariant functor captures the opposite case. If `F` is a contravariant functor, whenever we
> have a `F[A]` and a conversion `B => A` we can convert to an `F[B]`.
>
> Finally, invariant functors capture the case where we can convert from `F[A]` to `F[B]` via a
> function `A => B` and vice versa via a function `B => A`.
>

## Three types of functor

- Regular covariant(协变) functors, with their map method, represent the ability to apply functions
  to a value in some context. Successive calls to map apply these functions in sequence, each
  accepting the result of its predecessor as a parameter.
- Contravariant(逆变) functors, with their `contramap` method, represent the ability to "prepend"
  functions to a function-like context. Successive calls to contramap sequence these functions in
  the opposite order to map.
- Invariant(不变) functors, with their imap method, represent bidirectional(双向) transformations.

Regular Functors are by far the most common of these type classes, but even then it is rare to use
them on their own. Functors form a foundational building block of several more interesting
abstractions that we use all the time. In the following chapters we will look at two of these
abstractions: `monads` and `applicative` functors.

Functors for collections are extremely import, as they transform each element independently of the
rest. This allows us to parallelise or distribute transformations on large collections, a technique
leveraged heavily in "map-reduce" frameworks like *Hadoop*. We will investigate this approach in
more detail in the map-reduce case study later.

The Contravariant and In variant type classes are less widely applicable but are still useful for
building data types that represent transformations. We will revisit them to discuss
the `Semigroupal` type class later.
