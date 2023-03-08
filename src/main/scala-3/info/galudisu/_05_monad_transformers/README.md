# Monad Transformers

假设我们要操作数据库。需要查找user的记录。有可能记录不存在，返回一个`Option[User]`
。也有可能由于网络、权限等为题导致连接数据库失败，返回一个`Either[Error, Option[User]]`。

要使用到这个值，必须内嵌`flatMap`调用。

> **nest flatMap**
>
> 型如：
>
> ```scala
> flatMap {a =>
>   a.flatMap { b =>
>     b.flatMap { ??? }
>   }
> }
> ```
> 这种内嵌flatMap的语法，实际上等价于for-comprehension. for-comprehension实际就是内嵌flatMap的语法糖。
>

### Exercise: Composing Monads

A question arises. Given two arbitrary monads, can we combine them in some way to make a single
monad? That is, do monad `compose` ? We can try to write the code
, but we soon hit problems:

```scala
import cats.syntax.applicative._ // for pure

// Hypothetical example. This won't actually compile:
def compose[M1[_] : Monad, M2[_] : Monad] = {
  type Composed[A] = M1[M2[A]]

  new Monad[Composed] {
    def pure[A](a: A): Composed[A] = a.pure[M2].pure[M1]

    def flatMap[A, B](fa: Composed[A])(f: A => Composed[B]): Composed[B] =
    // Problem! How do we write flatMap?
      ???
  }
}
```

如果不知道`M1` 或 `M2`是啥，根本不可能实现`flatMap`的定义！

## A Transformative Example

Cats提供了常见ADT数据类型的monad转换，每个命名都有`T`后缀： `EitherT`为`Either`的组合monad，`OptionT`
为`Option`的组合。

譬如使用`OptionT`来组合`List`和`Option`类型。可以写成。

```scala
import cats.data.OptionT

type ListOption[T] = OptionT[List, A]
```

注意类型编程的入参格式为`OptionT[In, Out]`，传入的是`List`，它的内部monad为Option。

现在可以使用`OptionT`构造函数或`pure`来创建`ListOption`的实例了：

```scala
import cats.instances.list._ // for monad
import cats.syntax.applicative._ // for pure

val result1: ListOption[Int] = OptionT(List(Option(10)))
// result1: ListOption[Int] = OptionT(List(Some(10)))

val result2: ListOption[Int] = 32.pure[ListOption]
// result2: ListOption[Int] = OptionT(List(Some(32)))
```

`map` 和 `flatMap`融合了`List`和`Option`的对应操作：

```scala
result1.flatMap{ (x: Int) =>
  result2.map { (y: Int) =>
    x + y
  }
}
// res1: OptionT[List, Int] = OptionT(List(Some(42)))
```

This is the basis of all monad transformers. The combined `map` and `flatMap` methods allow us to
use both component monads without having to recursively unpack and repack values at each stage in
the computation. Now let's look at the API in more depth.

## Monad Transformers in Cats

Each monad transformer is a data type, defined in `cats.data`, that allows us to _wrap_ stacks of
monads to produce new monads. We use the monads we've built via the Monad type class. The main
concepts we have to cover to understand monad transformers are:

- the available transformer classes;
- how to build stacks of monads using transformers;
- how to construct instances of a monad stack; and
- how to pull apart a stack to access the wrapped monads.

cats 提供了内建的monad transformer data type。

### The Monad Transformer Classes

常用的monad transformer有：

- `cats.data.OptionT` for Option;
- `cats.data.EitherT` for Either;
- `cats.data.ReaderT` for Reader;
- `cats.data.WriterT` for Writer;
- `cats.data.StateT` for State;
- `cats.data.IdT` for the `Id` monad.

> **Kleisli Arrows**
>
> `Kleisli` 和 `ReaderT`实际上是同一样东西，`ReaderT`就是`Kleisli`的别名。

### Building Monad Stacks

All of these monad transformers follow the same convention. The transformer itself represents the
_inner_ monad in a stack, while the first type parameter specifiers the outer monad. The remaining
type parameters are the types we've used to form the corresponding monads.

monad transformer 表示栈上的一个monad，外部指定的type parameter，就是内部真正处理的类型。举个例子：

```scala
type ListOption[A] = OptionT[List, A]
```

参数类型`A`实际上就是内部Monad的参数类型。

Let's create a `Future` of an `Either` of `Option`. Once again we build this from the inside out
with an `OptionT` of an `EitherT` of `Future`. However, we can't define this in one line because `
EitherT has three type parameters:

```scala
case class EitherT[F[_], E, A](stack: F[Either[E, A]]) {
  // etc...
}
```

The three type parameters are as follows:

- `F[_]` is the outer monad in the stack(`Either` is the inner);
- `E` is the error type for the `Either`;
- `A` is the result type for the `Either`.

对于三层以上的transformer，可以通过类型编程的形式解决

```scala
import scala.concurrent.Future
import cats.data.{EitherT, OptionT}

type FutureEither[A] = EitherT[Future, String, A]
type FutureEitherOption[A] = OptionT[FutureEither, A]
```

三层以上的monad可以直接传递到for-comprehension里面：

```scala
import cats.instances.future._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

val futureEitherOr: FutureEitherOption[Int] =
  for {
    a <- 10.pure[FutureEitherOption]
    b <- 32.pure[FutureEitherOption]
  } yield a + b
```

### Constructing and Unpacking Instances

创建transformer monad有两种方式：`apply`和`pure`。

```scala
// Create using apply
val errorStack1 = OptionT[ErrorOr, Int](Right(Some(10)))

// create using pure:
val errorStack2 = 32.pure[ErrorOrOption]
```

unpack一个transformer实例比较简单，直接调用`.value`即可。

```scala
errorStack1.value

errorStack2.value.map(_.getOrElse(-1))
```

### Exercise: Monads: Transform and Roll Out

The Autobots(汽车人), well-known robots in disguise, frequently send messages during battle
requesting the power levels of their team mates. This helps them coordinate strategies and launch
devastating attacks. The message sending method looks like this:

```scala
def getPowerLevel(autobot: String): Response[Int] = ???
```

Transmissions take time in Earth's viscous atmosphere, and messages are occasionally lost due to
satellite malfunction or sabotage by pesky Decepticons(霸天虎). Responses are therefore represented
as a stack of monads:

```scala
type Response[A] = Future[Either[String, A]]
```

Optimus Prime(擎天柱) is getting tired of the nested for comprehensions in his neural matrix. Help
him by rewriting Response using a monad transformer.

Now test the code by implementing `getPowerLevel` to retrieve data from a set of imaginary allies.
Here's the data we'll use:

```scala
val powerLevels = Map(
  "Jazz"      -> 6,
  "Bumblebee" -> 8,
  "Hot Rod"   -> 10
)
```

基于探思探究的系统学习原则，下面我们手写`EitherT` transformer.

```scala
final case class EitherT[F[+_], A, B](val run: F[Either[A, B]])

//
type Response[A] = Either[Future, String, A]
```

`flatMap` 和 `map`具有传递性，因此，需要实现：

```scala
final case class EitherT[F[+_]: M, A, +B](val run: F[Either[A, B]]):
  def pure[AA >: A, D](value: AA): EitherT[F, AA, D] = ???
  def map[D](f: B => D): EitherT[F, A, D]            = ???
  /** It's left flatMap, but the behaviours same with rightFlatMap */
  def flatMap[AA >: A, D](f: B => EitherT[F, AA, D]): EitherT[F, AA, D] = ???

  def isLeft: F[Boolean]  = Monad[F].map(run)(_.isLeft)
  def isRight: F[Boolean] = Monad[F].map(run)(_.isRight)

object EitherT:
  def left[F[+_], A, B](fa: F[A])(using monad: Monad[F]): EitherT[F, A, B]  = EitherT(monad.map(fa)(Left(_)))
  def right[F[+_], A, B](fa: F[B])(using monad: Monad[F]): EitherT[F, A, B] = EitherT(monad.map(fa)(Right(_)))

```

Two autobots can perform a special move if their combined power level is grater than 15. Write a
second method, `canSpecialMove`, that accepts the names of two allies and checks whether a special
move is possible. If either ally is unavailable, fial with an appropriate error message:

```scala
def canSpecialMove(ally1: String, ally2: String): Response[Boolean] = ???
```

Finally, write a method `tacticalReport` that takes two ally names and prints a message saying
whether they can perform a special move:

```scala
def tacticalReport(ally1: String, ally2: String): String = ???
```

You should be able to use report as follows:

```scala
tacticalReport("Jazz", "Bumblebee")
// res13: String = "Jazz and Bumblebee need a recharge.
tacticalReport("Bumblebee", "Hot Rod")
// res14: String = "Bumblebee and Hot Rod are ready to roll out!
tacticalReport("Jazz", "Ironhide")
// res15: String = "Comms error: Ironhide unreachable"
```

# Summary

1. Monad transformers, eliminate(消除) the need for nested _for comprehensions_ and _pattern
   matching_ when working with "stacks" of nested monads.
2. The monad data type transformer, such as `FutureT`, `OptionT` or `EitherT`, provides the code
   needed to merge its related monad with other monads. The transformer is a data structure that
   wraps a monad stack, equipping it with `map` and `flatMap` methods that unpack and repack the
   whole stack.
3. The type signatures of monad transformers are written from the inside out, so
   an `EitherT[Option, String, A]` is a wrapper for an `Option[Either[String, A]]`. It is often
   useful to use type aliases when writing transformer types for deeply nested monads.

With this look at monad transformers, we have now covered everything we need to know about monads
and the sequencing of computations using `flatMap`. In the next chapter we will switch tack and
discuss two new type classes, `Semigroupal` and `Applicative`, that support new kinds of operation
such as zipping independent values within a context.

Monad transformer 自身也是monad，它的作用用于避免嵌套的 for-comprehension 和
模式匹配的糟糕语法。如果要给个合适的翻译，应该叫做 “monad alias”，类似于linux的alias概念。
