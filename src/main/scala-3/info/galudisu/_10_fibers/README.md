# Fibers

## Basic infrastructure

- IO Monad
- Future

### What is an effect/side-effect?

A side-effect is a function/expression which changes/mutates the state of something outside its
local environment.

It can be something as simple as printing to console, making a DB call or a file read/write
operation.

### What is an IO Monad?

IO Monad is simply a Monad which:

- Allows you to safely manipulate effects
- Transform the effects into data and further manipulate it before it actually gets evaluated.

As a side-note, the Monad also _guarantees sequential evaluation(保证顺序执行)_ during the
composition of 2 monads.

IO Monad 和 Free Monad的特性相同，但实现细节不同。

in the function: `f: A => F[B]`，we need the value of type `A` to transform it into `F[B]`, which
mandates(强制执行) that the evaluation of `F[A]` will be done before evaluation of `F[B]`
because `F[B]` won't even exist by then.

IO Monad 更多的是解决了 _referentially transparent_ 问题。IO Monad 其实就是 FreeMonad，由于
_referentially transparent_ 问题的存在，原来`Free`的结构ADT部分`Pure`、`Suspend`、`FlatMapped`
分别代表三个阶段，其中`Suspend`阶段仅有一个interpreter，也就是说Free
Monad的唯一的一个interpreter就是前面介绍的`Trampoline`。Free Monad更多的是解决 _Stack Safety_
，于是将`Suspend`部分进行分解。

下面是Cats关于IO monad进行分解伪代码。

```
            | Error
            | Delay                   | RealTime
Suspend  => | IO[FiniteDuration]  =>  | Monotonic
            | Attempt                 | ReadEC
            | Canceled
            | Uncancelable
            | Blocking
            | Exit
```

由此可见，IO Monad相当于把effect的控制权掌握在自己手里了。

The solution to our problem comes with the ability to *control* _WHEN_ these effects get
_evaluated_.

This is where the *IO Monad* comes in. The IO Monad can be thought of as a *wrapper around these
effects*, which provides us the ability to evaluate them when we need to. We'll be using the
cats-effect library in this post to demonstrate the use of IO Monad. So, an IO Monad embedding all
possible side effects **was never absolutely needed**.

```scala
def pointlessComputation: Int = {
  val array = new Array[Int](10)
  populate(array)  // mutable
  quicksort(array) // mutable
  array(0)
}
```

(Perhaps `Try` or `Either` could be considered IO monads for synchronous code).

> **什么是引用透明(referentially transparent)**
>
> _Haskell_ 作为纯函数语言，不存在referentially transparent说法，仅讨论Scala。
>
> By definition, it is a property of purely functional languages that says an expression always gets
> evaluated to the same result, and that the expression itself can be replaced by its result without
> changing the behavior of the program.

### Shortcomings of Future

The `Future` issues showing as bellow:

```scala
def test(): Future[Int] = Future {
  sleep(1000)
  println("running")
  1
}
```

由于Future是线程绑定的，下面代码是顺序执行的，

```scala
for {
  a <- test()
  b <- test()
} yield a + b
```

而下面会计算得到结果2，并行执行。

```scala
val fa = test()
val fb = test()

for {
  a <- fa
  b <- fb
} yield a + b
```

换句话说，`Future`是 **eagerly calculated**(might start as soon as you created it the future begins
now) and **memoize**(remembers/caches) results, so all side effects are run once.

`Future`属于一种“饥饿”型计算，被创建后立即/随后被执行，执行得到的结果被缓存。也就是说：

1. 不知道它什么时候结束，你必须时刻惦记拿到值后记得处理。
2. `Future` 的结果仅被处理一次！你必须“假设”它所有可能的结果，它的异常是未知的。这对于重构来说是不友好的。
3. `Future` _不是一个合法的monad_ ，因为它根本不遵循monad law.

```scala
val f: Int => Future[String] = _ => throw new Exception
// for Future unit=successful
Future.successful(1) flatMap f // Failed Future
// !=
(f flatMap Future.successful)(1) // thrown Exception
```

4. `Future` 在Real world concurrency中常常出现一连串`Future`的场景，这个数量无法预估，也无法处理。

```scala
val pending: List[Future] = ???
```

5. `Future` 由于Future仅被执行一次，在真实世界中，Success 和 Failure 并不是顺序到达的。

### Imperative programming in Haskell

Haskell 是一门纯函数、命令式的语言。意味着，你不能通过`StdIn.readLine`然后获得用户输入的`String`
。相反，该语言会强制你使用pure values and function。也就不会有 side effect的说法。但是，**Side Effect**
就是我们的 business value !!! 这就引入了`IO`。

相比Cats 和 Scalaz。推荐学习Cats，因为Scalaz notoriously
considered [Haskell resources](https://www.haskell.org/documentation/) (
优秀的艺术家抄袭，伟大的艺术家剽窃)。实际上Cats和Scalaz是对FP的相互补充。

### IO Monad

根据Cats Effects 2.x 的官方介绍。

`IO` A data type for encoding side effects as pure values, capable of expressing both synchronous
and asynchronous computations.

譬如类型`IO[A]`，它可以对effect进行 evaluated, compute！！ 也就是说，`IO`内部通过“织入” side
effect方式，保留了 _referential transparency_ 的计算。因此，一个`IO` 包含两部分计算，synchronous 或
asynchronous，其中：

1. on evaluation yield exactly one result
2. can end in either success or failure and in case of failure `flatMap` chains get
   short-circuited(`IO` implementing the algebra of `MonadError`)
3. can be canceled, but note this capability relies on the user to provide cancellation logic

|                | Eager                 | Lazy                        |
|:---------------|:----------------------|:----------------------------|
| `Synchronous`  | `A`                   | `() => A`                   |
|                |                       | `Eval[A]`                   |
| `Asynchronous` | `(A => Unit) => Unit` | `() => (A => Unit) => Unit` |
|                | `Future[A]`           | `IO[A]`                     |

### Fibers

> 在Fibers中，并发是抽象的。
