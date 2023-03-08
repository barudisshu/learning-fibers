# features

Scala 3是scala语言的大改革(overhaul)
。核心部分，类型系统层面变的更有原则。同时也带来了令人兴奋的新特性，最紧要的是，意味着类型系统的阻碍更少，类型推断和重载方案更加高效。

Scala 3使用了下一代编译器Dotty，提供了更多的语言特性和改进。

什么是DOT和Dotty？

Scala语言由三个元素组成：

- Types
- Functions
- Objects

其它语言为这些元素的子集。譬如Common Lisp(function and object)，Java(type and object)，Haskell和ML(
functions and types) -
Scala是第一个这三者结合的语言。Scala3以typed level层面向OOP和FP迈出了明晰的一大步。因此，DOT计算(
DOT=OOP+FP, calculus)正是以此立基。

Scala 3开始引入了DOT(Dependent Object Types)。DOT背后的哲学理念是捕获Scala的本质向简洁和强大迈进。

**Dotty是Scala3开发版的一个编译器，使用DOT Calculus**。DOT属于type-calculus。

Scala 2和Scala 3基本属于同类语言。但Scala3的编译器背后是Dotty。Scala 2则是 _scalac_，Scala 3对应
_dotc_。

Scala 3舍弃掉了一些不健全的、无用的特性，以使得语言尽量小和更常规化。它添加了一些新的结构来增加它的表述能力。同时也改变了一些结构使其更简单，一致，易用。

# type system

Scala 3的类型系统更简单、合理、一致。

## existential types

**Existential Types (`T forSome { type X}`) and Type Projections (`T#A`) are dropped**
。存在类型和投影类型被丢弃了。它们是不健全的，和其它结构交互变得非常困难。不过对于具体类型的投影仍然支持。

## intersection and union types

**Compound Types (`T with U`) replaced with Intersection Types (`T & U`) and Union Types (`T | U`)**
。复合类型(compound type)
被拆分为了交集类型(intersection type)和并集类型(union type)，scala的子类型层级(subtype hierarchy)
变成了格子(lattice)，这样令编译器更容易 **find the least
upper bounds and greatest lower bounds**。这对于编译器进行类型推导变得更健全。

下面定义一个`parse`方法并返回一个union type：

```scala
def parse(input: String): Int | String =
  try input.toInt
  catch
case _ => "Not a number"

```

union type `A|B` 包含了`A`类型的所有值，也包含`B`类型的所有值。**Unions are duals intersection types**
。并集具有互换性，因此`A|B`等价于`B|A`
。我们可以使用模式匹配来决定`A|B`的值是`A`还是`B`。

下面是交集类型(intersection type)：

```scala
trait Show

:
def show: String

trait Closable

:
def close: Unit

type Resource = Show & Closable

def shutdown(resource: Resource) =
  println(s"Closing resource ${resource.show}")
resource.close

object res extends Show

, Cloneable:
override def sow = "resource#1"
override def close = println("Resource closed!")

shutdown(res)
```

类型`A & B`的值同时表达了`A`和`B`类型的值。`A & B`包含了`A`和`B`
的所有成员和属性。交集具有互换性，意味着`A & B`等价于`B & A`，但是`with`复合类型不具有互换性。

## type lambda

**Type Lambda is introduced with a nice syntax, `[X] =>> F[X]`**。λ表达式引入了一个非常好的语法。它是一个高阶类型。

```scala
import scala.util.Either

trait Monad[F[_]] {
  def pure[A](x: A): F[A]

  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
}

trait EitherInstance[E] extends Monad[

[T
] =>> Either
[E
, T
]]
{
  override def pure[A](x: A) = ???

  override def flatMap[A, B](fa: Either[E, A])(f: A => Either[E, B]) = ???
}
```

如果用scala 2的写法看起来非常糟糕，因为它用到了类型投影操作符`#`(其实也可以不用。。。)：

```scala
trait EitherInstance[E] extends Monad[({type T[X] = Either[E, X]})#T] {
  override def pure[A](x: A) = ???

  override def flatMap[A, B](fa: Either[E, A])(f: A => Either[E, B]) = ???
}
```

# traits

截止Scala 2，我们都没有办法给一个trait传递参数。现在可以把trait作为抽象类并在子类预先填充参数。

这种方案不会引入 **过早初始化(early initialization)** 的问题。

```scala
trait Base(val msg: String)

class A extends Base("Foo")

class B extends Base("Bar")

```

**Traits, like classes, can have parameters. Their arguments are evaluated before the trait is
initialized.**

> Kotlin中这种预设参数的形式有些许不同：
>
> 下面是kotlin的写法
>
> ```kt
> interface Base {
>   val msg: String
> }
> ```
>
> ```kt
> class A(override val msg: String): Base   // 这里被称为primary constructor
> ```
>
> 或写成
>
> ```kt
> class A {
>   constructor(msg: String) {...} // secondary constructor
> }
> ```
>
> ```kt
> A("Foo")
> ```

# enums

Scala 3之前，编写枚举是非常尴尬的。为了编写一个简单的ADT会产生非常多的样板代码。缺乏表述性。Scala
3内置了`enum`关键字帮助我们编写ADT的枚举。使用`enum`，我们可以定义类型的集合枚举。

```scala
enum Color:
case Red
, Green
, Blue
```

如果要兼容Java枚举，我们可以继承`java.lang.Enum`(没什么卵用... 不会有人跟java集成吧，不会吧)：

```scala
enum Color
extends java.lang.Enum[color]:
case Red
, Green
, Blue
```

可以参数化：

```scala
enum Color (code: String):
case Red
extends Color("#FF0000")
case Green
extends Color("#00FF00")
case Blue
extends Color("#0000FF")
```

也可以编写ADT类型枚举：

```scala
enum Option
[+T
]:
case Some(x: T)
case None
```

# implicits

**Implicits have too many puzzlers in Scala 2 - there's too much head-scratching**
。implicit有时非常难以理解，易出错的、易误用的、过度使用的，有许多粗糙的边缘。他们有些是不可避免的。

在Scala 2中隐式被大量使用。让我们列举一下一些重要的：

- 提供上下文环境
- 编写类型实例
- 扩展方法
- 隐式转换

Scala 2的隐式传递机制多余继承。Scala 3关注于继承以及引入新的关键字`given`和`using`的机制进行代替。但为了兼容，Scala
2的隐式依然有效。

## providing contextual environment

Scala 2中通过隐式提供一种上下文环境，例如常见的Akka中的`ExecutionContext`、`ActorSystem`：

```scala
import scala.concurrent._
import scala.concurrent.duration._

implicit val ec: scala.concurrent.ExecutionContext = ExecutionContext.global

def square(i: Int)(implicit
val ec: ExecutionContext
): Future[Int] = Future(i * i)
```

Scala 3使用`using`和`given`关键字来传递上下文：

```scala
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._

given ExecutionContext = ExecutionContext.global

import scala.concurrent.Future

def square(i: Int)(using ec: ExecutionContext
): Future[Int] =
  Future(i * i)
```

## writing type class instances

最传统的做法是传递实例：

```scala
trait Ord[T] {
  def compose(x: T, y: T): Int
}

implicit intInstance: Ord[Int]
= new Ord[Int] {
  override def compose(x: Int, y: Int): Int = if (x < y) -1 else if (x > y) +1 else 0
}
```

Scala 3中`implicit` 关键字替换为`using`关键字，

```scala
trait Ord[T]

:
def compare(x: T, y: T): Int

given Ord
[Int
] with
override def compose(x: Int, y: Int) =
  if (x < y) -1 else if (x > y) +1 else 0
```

## extension methods

Scala 2中编写扩展方法有些许样板代码。我们需要用到隐式函数编写它的包装类型，将类型包装到新的扩展类上：

```scala
import scala.language.implicitConversions

class RichInt(i: Int) {
  def square = i * i
}

object RichInt {
  implicit def richInt(i: Int): RichInt = new RichInt(i)
}

```

Scala 3有一个特定的关键字——`extension`，用于编写扩展方法的简洁语法：

```scala
extension(i: Int)
def square = i * i
```

> 对比一下Kotlin的extension function的写法：
>
> ```kt
> fun Int.square() = this * this
> ```

## implicit conversions

在Scala 2中，隐式转换有时是未知bug的根源，所以我们需要特别小心地使用它。

```scala
import scala.language.implicitConversions

implicit def string2Int(str: String): Int = Integr.parseInt(str)
def square(i: Int) = i * i
```

在Scala 3中写法不同，我们需要提供`Conversion[A, B]`的一个实例：

```scala
import scala.language.implicitConversions

given Conversion
[String
, Int
] = Integer.parseInt(_)
def square(i: Int) = i * i
```

**Implicit conversions in Scala 3 are hard to misuse.** Scala 3这种写法相比Scala 2减少出错几率。
