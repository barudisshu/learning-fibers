# Free Monad

__A Generalization of Trampoline__

> The core idea of Free is to switch from effectful functions (that might be impure) to plain data
> structures that represents our domain logic. Those data structures are composed in a structure that
> is free to interpretation and the exact implementation of our program can be decided later.
>
> 一句话，`Trampoline`就是将`flatMap(flatMap(flatMap(...)))`
> 方法栈空间以尾递归的形式转换为`FlatMap(FlatMap(FlatMap(...)))`
> 的形式，因为是尾递归优化，堆空间的`FlatMap`“相当于(其实不是，具体看编译器尾递归如何优化)
> ”对象仅创建一次，时序执行指令得到最优解。`FreeMonad`的定义为 _a generalization of trampoline_
> ，在原来Stack Safety的基础上，通过代码层面的`interpreter`方式，将原本带有`mutation`(可变性)
> 和`side effects`(副作用)的部分，转变为Data type对象类型，也就是将不纯的函数，转变为了结构体。
>


[Typeclass hierarchy](https://blog.rockthejvm.com/cats-typeclass-hierarchy/)
