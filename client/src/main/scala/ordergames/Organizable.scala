package ordergames

import com.thoughtworks.binding.Binding.Vars

trait WithOrdering[A] { self: A =>
  def ordering: Int
  def withNewOrder(order: Int): A
}
class Organizable[ T <: WithOrdering[T]](elements: Vars[T]) {
  val sortedElements: Vars[T] = Vars.empty[T]
  def updateSortedElements(): Unit = {
    sortedElements.value.clear()
    sortedElements.value.appendAll(elements.value.sortBy(_.ordering))
  }
  def assignHigherOrder(e: T): Unit = elements.value.filter(_.ordering > e.ordering).minByOption(_.ordering).foreach(t => swapOrdering(e,t))
  def assignLowerOrder(e: T): Unit = elements.value.filter(_.ordering < e.ordering).maxByOption(_.ordering).foreach(t => swapOrdering(e,t))

  private def swapOrdering(t0: T, t1: T): Unit = {
    val p0 = elements.value.indexOf(t0)
    val p1 = elements.value.indexOf(t1)
    elements.value.update(p0,t0.withNewOrder(t1.ordering))
    elements.value.update(p1,t1.withNewOrder(t0.ordering))
    updateSortedElements()
  }
  updateSortedElements()




}
