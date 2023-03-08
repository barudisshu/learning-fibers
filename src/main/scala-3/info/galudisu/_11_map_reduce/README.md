# Map-Reduce

We're going to implement a simple-but-powerful parallel processing framework
using `Monoids`, `Functors`, and a host of other goodies.

*MapReduce*, which is a programming model for doing parallel data processing across clusters of
machines(aka "nodes"). As the name suggest, the model is built around a _map_ phase, which is the
same `map` function we know from Scala and the `Functor` type class, and a _reduce_ phase, which we
usually call `fold` in Scala.

## Parallelizing `map` and `fold`

