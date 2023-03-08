# Monoids and Semigroups

## Definition of a Monoid

定义：对于类型`A`的一个幺半群Monoid：

- 二元操作：`(A,A) => A`
- 单位元（幺元）：`unit: A`

幺半群monoid **必须** 遵循以下几个法则.

1. associative：对于任何`A`中的元素`x`，`y`和`z`，`(x <+> y) <+> z == x <+> (y <+> z)`，这里的`<+>`二元操作。
2. identity：对于任何`A`中的元素`x`和单位元的二元操作都是它自身。

> **对于二元运算的命名**
>
> 按照严格的学术定义，对于非空集合`G`，定义G上的二元操作`*`，满足
>
> 封闭性(Closure)：对于任意`a，b∈G`，有`a*b∈G`
> 结合律(Associativity)：对于任意`a，b，c∈G`，有`(a*b)*c=a*(b*c)`
> 幺元(Identity)：存在幺元`e`，使得对于任意`a∈G，e*a=a*e=a`
> 逆元：对于任意`a∈G`，存在逆元`a^-1`，使得`a^-1*a=a*a^-1=e`
>
> 则称`(G，*)`是群，简称`G`是群。
>
> 这里的二元操作在英文命名中有很多写法，譬如`combine`、`append`、`op`
> 。有些更高阶的定义如包含特殊含义应该命名更具体一点，譬如`def join[A]` `def intersect[A]`
> 。必须严格区分命名并用在正确的地方。

定义：对于类型`A`的一个半群Semigroup：

- 映射：`(A,A) => A`

半群相对于幺半群来说就是不包含单位元。一个Monoid又可以写成：

```scala
trait Monoid[A] extends Semigroup[A] {
  def unit: A
}
```



