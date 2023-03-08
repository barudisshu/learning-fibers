# Monads

单体

## Monad

While we have only talked about `flatMap` above, monadic(单孢体的) behaviour is formally captured in
two operations:

- `pure`, of type `A => F[A]`;
- `flatMap`, of type `(F[A], A => F[B]) => F[B]`.

pure abstracts over constructors, providing a way to create a new monadic context from a plain
value. flatMap provides the sequencing step we have already discussed, extracting the value from a
context and generating the next context in the sequence.

> 在某些库或语言中，尤其是Scalaz和Haskell，`pure`被称为`point`或`return`，`flatMap`被称为`bind`或`>>=`
> 。仅仅是术语上的不同。

```scala
trait Monad[F[_]]:
  def pure[A](value: A): F[A]
  def flatMap[A, B](value: F[A])(func: A => F[B]): F[B]
```

> ** Monad Laws**
>
> `pure`和`flatMap`必须遵循一系列的法则，以允许自由地序列操作的同时不会产生副作用和意想不到的的意外。
>
> _Left identity_: calling `pure` and transforming the result with `func` is the same as
> calling `func`:
>
> ```scala
> pure(a).flatMap(func) == func(a)
> ```
>
> _Right identity_: passing `pure` to `flatMap` is the same as doing nothing:
>
> ```scala
> m.flatMap(pure) == m
> ```
>
> _Associativity_: flatMapping over two functions `f` and `g` is the same as flatMapping over `f`
> and then flatMapping over `g`:
>
> ```scala
> m.flatMap(f).flatMap(g) == m.flatMap(x => f(x).flatMap(g))
> ```

## Exercise: Getting Func-y

Every monad is also a functor. We can define map in the same way for every monad using the existing
methods, `flatMap` and `pure`:

```scala
trait Monad[F[_]]:
  def pure[A](a: A): F[A]
  def flatMap[A, B](value: F[A])(func: A => F[B]): F[B]
  def map[A, B](value: F[A])(func: A => B): F[B] = ???
```

Functor 有的功能，Monad都有。

## Monad within for-comprehension

在介绍Free Monad之前，首先介绍尾调用优化问题。

### Trampolining and stack safety in Scala

Scala编译器只有尾递归优化，但是没有尾递归消除技术。这是因为对于不包含副作用的递归函数，是可以进行递归优化的。

```scala
def fib(num: Int): BigInt = {
  @scala.annotation.tailrec
  def _fib(n: Int, acc1: BigInt, acc2: BigInt): BigInt = n match {
    case 0 => acc1
    case 1 => acc2
    case _ => _fib(n - 1, acc2, acc1 + acc2)
  }
  _fib(num, 0, 1)
}
```

但是对于组合函数来说，不能确保其它函数是否会带来side effect，譬如IO函数、Net相关函数等极其容易在递归调用中出现stack
overflow，也就是“栈帧(stack frame)”[溢出](https://en.wikipedia.org/wiki/Tail_call)。

```scala
def even[A](ls: List[A]): Boolean =
  ls match {
    case Nil     => true
    case x :: xs => odd(xs)
  }
def odd[A](ls: List[A]): Boolean =
  ls match {
    case Nil     => false
    case x :: xs => even(xs)
  }

even((0 to 1_000_000).toList) // 爆栈！！
```

目前为止，Scala 编译器对尾调用的唯一优化技术是“尾递归调用(self-recursive call)”：

```scala
def gcd(a: Int, b: Int): Int =
  if (b == 0) a else gcd(b, a % b)
```

> We need a way to transform every call into a __self-recursive call__ that compiler will optimize.

**解决思路**

将多个函数的调用，转换成单一函数的self-recursive function，这将引入“弹床”。

> [ZIO](https://zio.dev/) 是一种类似于IO Free的实现框架，解决了这种side
> effect带来的问题，其中引入了Fiber、for-comprehension(flatMap)等相关概念进行blueprint的构建。

### Trampoline: Making every call a self recursive tail call

首先，设计能代表递归程序的ADT。

```scala
sealed trait Trampoline[A]
case class Done[A](value: A) extends Trampoline[A]
case class More[A](call: () => Trampoline[A]) extends Trampoline[A]
```

- `Done` if there are no computations to be done, and we can yield a value
- `More` if there is a recursive function call to be made

下面一个例子，表示有2个suspended计算并yield值42.

```scala
More(() => More(() => Done(42)))
```

改写原来的递归调用函数：

```scala
def even[A](ls: Seq[A]): Trampoline[Boolean] =
  ls match {
    case Nil     => Done(true)
    case x :: xs => More(() => odd(xs))
  }
def odd[A](ls: Seq[A]): Trampoline[Boolean] =
  ls match {
    case Nil     => Done(false)
    case x :: xs => More(() => even(xs))
  }
```

要获取实际计算的值，我们需要转换调用。

```scala
def run[A](trampoline: Trampoline[A]): A =
  trampoline match {
    case Done(v) => v
    case More(t) => run(t())
  }
```

另一种重构优化方式采用分治方法对递归进行快速“回溯”。

```scala
def resume[A](t: Trampoline[A]): Either[() => Trampoline[A], A] =
  t match {
    case Done(v) => Right(v)
    case More(k) => Left(k)
  }

def run[A](t: Trampoline[A]): A = resume(t) match {
  case Right(value) => value
  case Left(more) => run(more())
}
```

为了方便，将这些函数嵌入到`Trampoline`里面：

```scala
sealed trait Trampoline[+A]:
  def resume: Either[() => Trampoline[A], A] = this match {
    case Done(v) => Right(v)
    case More(k) => Left(k)
  }

  @scala.annotation.tailrec
  final def runT: A = resume match {
    case Right(value) => value
    case Left(more)   => more().runT
  }
```

堆栈溢出仍然存在...

### State Monad

首先定义如下的state monad：

```scala
case class State[S, +A](runS: S => (S, A)) {
  def map[B](f: A => B): State[S, B] =
    State[S, B] { s =>
      val (s1, a) = runS(s)
      (s1, f(a))
    }
  def flatMap[B](f: A => State[S, B]): State[S, B] =
    State[S, B] { s =>
      val (s1, a) = runS(s)
      f(a).runS(s1)
    }
}
def getState[S]: State[S, S]           = State(s => (s, s))
def setState[S](s: S): State[S, Unit]  = State(_ => (s, ()))
def pureState[S, A](a: A): State[S, A] = State(s => (s, a))
```

一个错误的用法如下：

```scala
def zipIndex[A](as: Seq[A]): State[Int, List[(Int, A)]] =
  as.foldLeft(pureState[Int, List[(Int, A)]](List()))((acc, a) =>
    for {
      xs <- acc
      n  <- getState
      _  <- setState(n + 1)
    } yield (n, a) :: xs
  )

zipIndex(0 to 1_000_000).runS(0)
```

看起来没什么问题，实际上在执行期间调用了多次`flatMap`的所有栈内存上。

```scala
flatMap(flatMap(flatMap(...
```

问题很明显，`flatMap`不是self-recursive tail 调用。一种方式是引入“弹床”，更改如下：

```scala
case class State[S, +A](runS: S => Trampoline[(S, A)]) {
  def flatMap[B](f: A => State[S, B]): State[S, B] =
    State[S, B](s => More(() => {
      val (s1, a) = runS(s).runT // <- not in the tail position
      More(() => f(a) runS s1)
    }))
}
```

`runS`必须要在`s`返回`More`之前执行一次，也就意味着使用“弹床”这个把戏无效（哦豁...

与其去修复`flatMap`的实现，我们仅需要暂停(suspend)整个`flatMap`的操作，知道`run`函数和`resume`
函数被调用时再对其进行解绑。

```scala
sealed trait Trampoline[A]

case class Done[A](value: A) extends Trampoline[A]

case class More[A](call: () => Trampoline[A]) extends Trampoline[A]

case class FlatMap[A, B](
  sub: Trampoline[A],
  cont: A => Trampoline[B]) extends Trampoline[B]
```

之后State Monad的`map`和`flatMap`更改为：

```scala
case class State[S, +A](runS: S => Trampoline[(S, A)]) {
  def map[B](f: A => B): State[S, B] = State[S, B](
    runS.andThen { tramp =>
      val (s, a) = tramp.runT
      Done((s, f(a)))
    }
  )
  def flatMap[B](f: A => State[S, B]): State[S, B] = State[S, B](
    runS.andThen { tramp =>
      FlatMap[(S, A), (S, B)](tramp, { case (s, a) => f(a).runS(s) })
    }
  )
}
```

该方式将原本的`flatMap(flatMap(flatMap...))`的栈调用方式，转变为了`FlatMap(FlatMap(FlatMap(...)))`
的堆空间对象!!!

下面是重构后的`Trampoline`代码。

```scala
sealed trait Trampoline[+A]:
  def resume: Either[() => Trampoline[A], A] = this match {
    case Done(v) => Right(v)
    case More(k) => Left(k)
    case FlatMap(sub, cont) =>
      sub match {
        case Done(v) => cont(v).resume
        case More(k) => Left(() => FlatMap(k(), cont))
        case FlatMap(sub2, cont2) =>
          (FlatMap(sub2, x => FlatMap(cont2(x), cont)): Trampoline[A]).resume
      }
  }

  @scala.annotation.tailrec
  final def runT: A = resume match {
    case Right(value) => value
    case Left(more)   => more().runT
  }
```

其中`FlatMap`有`sub`(sub expression)和`cont`(continuation function):

- `Done(v)`表示`sub`是`FlatMaps`链的最后一个，只需要透过`cont`将`v`传递给`resume`。
- `More(k)`(其中`k`是一个suspend函数)，意味着某个步骤可以调用`k`，并用`FlatMap`包装`cont`和`k`
- `FlatMap(sub2, cont2)`中创建了下一个`sub2`的表达式。

仍然有堆栈溢出问题存在...

如果`FlatMap`内嵌得足够深的话，由调用链中的一个`sub`触发另一个`sub`
也可能会触发堆栈溢出。给`Trampoline`也提供一个`flatMap`的binding。

```scala
sealed trait Trampoline[+A]:
  /* resume and runT */
  def flatMap[B](f: A => Trampoline[B]): Trampoline[B] = More[B](() => f(runT))
```

重构State Monad的代码：

```scala
case class State[S, +A](runS: S => Trampoline[(S, A)]) {
  def map[B](f: A => B): State[S, B] = State[S, B](
    runS.andThen { tramp =>
      val (s, a) = tramp.runT
      Done((s, f(a)))
    }
  )
  def flatMap[B](f: A => State[S, B]): State[S, B] = State[S, B](
    runS.andThen(tramp => tramp.flatMap { case (s, a) => f(a).runS(s) })
  )
}
```

Also see [Stackless Scala With Free Monads](http://blog.higher-order.com/assets/trampolines.pdf)

### Free monad: A generalization of Trampoline

What's problem of monad. Stack-Safety.

- represent stateful computations as data, and run them
- run recursive computations in a stack-safe way
- build an embedded DSL (domain-specific language)
- retarget a computation to another interpreter using natural transformations

> The core idea of Free is to switch from effectful functions (that might be impure) to plain data
> structures that represents our domain logic. Those data structures are composed in a structure that
> is free to interpretation and the exact implementation of our program can be decided later.



> **for-comprehension**
>
> for-comprehension is also the monad syntax sugar
>
> ```scala
> val result = for {
>   x <- List(1, 2, 3)
>   y <- List(4, 5, 6)
> } yield x + y
> ```
>
> equivalent
>
> ```scala
> List(1, 2, 3).flatMap(x => List(4, 5, 6).map(y => x + y))
> ```
>

Also see chapter09 && chapter10
