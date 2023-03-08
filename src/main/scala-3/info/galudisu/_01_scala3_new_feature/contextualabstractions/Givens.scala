package info.galudisu._01_scala3_new_feature.contextualabstractions

object Givens extends App {

  given priceOrdering: Ordering[Item] = (x: Item, y: Item) => x.price.compareTo(y.price)

  given pageLimit: Int = 2

  // Anonymous Givens
  given Item = Item("Dummy", 0.0)

  case class Item(name: String, price: Double)
}
