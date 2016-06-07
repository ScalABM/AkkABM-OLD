import markets.engines.orderbooks.SortedOrderBook
import markets.engines.orderbooks.immutable.ImmutableOrderBook
import markets.orders.Order

import scala.collection.immutable

class ImmutableTreeSetOrderBook[A <: Order](val ordering: Ordering[A])
  extends ImmutableOrderBook[A, immutable.TreeSet[A]]
    with SortedOrderBook[A, immutable.TreeSet[A]] {

  /** Add an order to the order book.
    *
    * @param order the order that is to be added to the order book.
    * @note adding an order is an O(log n) operation.
    */
  def add(order: A): Unit = {
    backingStore = backingStore + order
  }

  /** Remove an order from the order book.
    *
    * @param order the order that is to be removed from the order book.
    * @return true if the order is removed; false otherwise.
    * @note removing an order is an O(log n) operation.
    */
  def remove(order: A): Unit = {
    backingStore = backingStore - order
  }

  protected var backingStore = immutable.TreeSet.empty[A](ordering)

}
