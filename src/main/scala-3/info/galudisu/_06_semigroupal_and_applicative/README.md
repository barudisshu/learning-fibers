# Semigroupal and Applicative

Functors and monads let us sequence operations using `map` and `flatMap`. While functors and monads
are both immensely(极其) useful abstractions, there are certain types of program flow(我理解它应该指类似于
workflow 工作流，wizard，又或akka stream之类具有pending的情况) that they cannot represent.

很明显的例子就是表单验证。表单的验证不会因为因为第一个出现的错误就返回给用户，而是汇聚所有的错误信息。如果采取`EitherT`
，会导致fail fast and lose errors。

```scala
import cats.syntax.either._ // for catchOnly

def parseInt(str: String): Either[String, Int] =
  Either.catchOnly[NumberFormatException](str.toInt).leftMap(_ => s"Couldn't read $str")

for {
  a <- parseInt("a")
  b <- parseInt("b")
  c <- parseInt("c")
} yield a + b +c
// res0: Either[String, Int] = Left("Couldn't read a")
```

另一个例子是`Future`的并发求值。如果有几个long-runtime task，它们是并发执行的。然后，monadic
comprehension仅允许它们是按顺序执行。`map`和`flatMap`不能很好捕获我们想要的，因为每个计算都取决于前一次的计算值。

```scala
// context2 is dependent on value1
context1.flatMap(value1 => context2)
```

The calls to `parseInt` and `Future` above are _independent_ of one another, but `map` and `flatMap`
can't exploit this. We need a weaker construct-one that doesn't guarantee sequencing-to achieve the
result we want. In this chapter we will look at three type classes that support this pattern:

- `Semigroupal`, encompasses the notion of composing pairs of contexts.
- `Parallel`, converts types with a `Monad` instance to a related type with a `Semigroupal`
  instance.
- `Applicative`, tends `Semigroupal` and `Functor`. It provides a way of applying functions to
  parameters within a context.

# Semigroupal

`Semigroupal` 不是Semigroup(半群)，后缀-pal构成名词，表示一种能力。

```scala
trait Semigroupal[F[_]]:
def product[A, B](fa: F[A], fb: F[B]): F[(A, B)]

object Semigroupal:
def apply[F[_]](using semigroupal: Semigroupal[F]): Semigroupal[F] = semigroupal
```

### Semigroupal Laws

There is only one law for Semigroupal: The product method(乘法) must be associative.

```scala
product(a, product(b, c)) == product(product(a, b), c)
```

_乘法必须满足交换律_.

## Apply Syntax

所有关于cats.syntax._ 部分不作介绍，无意义(这种语法一定程度上破坏了代码的可读性，为什么不去掉？)。

(略)

## Fancy Functors and Apply Syntax

(略)

### Exercise: The Product of Lists

笛卡尔积(Cartesian product)。

```scala
Semigroupal[List].product(List(1, 2), List(3, 4))
// res5: List[(Int, Int)] = List(1, 3), (1, 4), (2, 3), (2, 4))
```

# Parallel

In the previous section we saw that when call `product` on a type that has a `Monad` instance we get
sequential semantics(顺序语义). This makes sense from the point-of-view of keeping consistency with
implementations of `product` in terms of `flatMap` and `map`. However, it's not always what we want.
The `Parallel` type class, and its associated syntax, allows us to access alternate semantics for
certain monads.

The definition below is the core of `Parallel`.

```scala
trait Parallel[M[_]] {
  type F[_]

  def applicative: Applicative[F]
  def monad: Monad[M]
  def parallel: ~>[M, F]
}
```

This tells us if there is a `Parallel` instance for some type constructor `M` then:

- there must be a `Monad` instance for `M`;
- there is a related type constructor `F` that has an `Applicative` instance;
- we can convert `M` to `F`.

`~>` 是`FunctionK`的别名，表示`M` to `F`。最常见的函数`A => B`就是将类型`A`转换为`B`的形式。定义如下：

```scala
trait ~>[F[_], G[_]]:
  def apply[A](fa: F[A]): G[A]
```

# Apply and Applicative

`Applicative` 就是将`A` => `F[A]`的函数。定义如下：

```scala
trait Application[F[_]] extends Apply[F]:
  def pure[A](a: A): F[A]
```

### The Hierarchy of Sequencing Type Classes

Each type class in the hierarchy represents a particular set of sequencing semantics, introduces a
set of characteristic methods, and defines the functionality of its supertypes in terms of them:

- every `monad` is an `applicative`;
- every `applicative` a `semigroupal`;
- and so on.

Because of the lawful nature of the relationships between the type classes, the inheritance
relationships are constant across all instances of a type class.

![Figure 6.1: Monad type class hierarchy](/img/figure_06_01.png)

`Apply` defines product in terms of `ap` and `map`; `Monad` defines `procuct`,`ap`,and `map`, in
terms of `pure` and `flatMap`.

To illustrate this let's consider two hypothetical data types:

- `Foo` is a monad. It has an instance of the `Monad` type class that implements `pure`
  and `flatMap` and inherits standard definitions of `product`,`map`, and `ap`;
- `Bar` is an applicative functor. It has an instance of `Applicative` that implements `pure`
  and `ap` and inherits standard definitions of `product` and `map`.

# Summary

While monads and functors are the most widely used sequencing data types we've covered in this book,
semigroupals and applicatives are the most general. These type classes provide a generic mechanism
to combine values and apply functions within a context, from which we can fashion monads and a
variety of other combinators.

`Semigroupal` and `Applicative` are most commonly used as a means of combining independent values
such as the results of validation rules. Cats provides the `Validated` type for this specific
purpose, along with apply syntax as a convenient way to express the combination of rules.
