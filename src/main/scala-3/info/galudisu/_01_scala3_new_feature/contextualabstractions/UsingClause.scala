package info.galudisu._01_scala3_new_feature.contextualabstractions

import info.galudisu._01_scala3_new_feature.contextualabstractions.Givens.Item
import info.galudisu._01_scala3_new_feature.contextualabstractions.Givens.pageLimit
import info.galudisu._01_scala3_new_feature.contextualabstractions.Givens.priceOrdering

object UsingClause extends App {

  val shoppingCart = List(
    Item("PanCake", 4),
    Item("Coke", 1),
    Item("Pizza", 5),
    Item("Burger", 3)
  )
  val sortedItems = listItems(shoppingCart)

  def listItems(products: Seq[Item])(using ordering: Ordering[Item])(using limit: Int) =
    products.sorted.take(limit)
  println(sortedItems) // prints -> List(Item(Coke, 1.0), Item(Burger,3.0))

}
