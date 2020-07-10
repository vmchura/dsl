package shared.utils

object Util {

  def LCSLength(X: String, Y: String): Int = {
    val C = Array.fill(X.length+1,Y.length+1)(0)
    for{
      i <- X.indices
      j <- Y.indices
    }yield{
      C(i+1)(j+1) = if(X(i) == Y(j)){
        C(i)(j) + 1
      }else{
        Math.max(C(i+1)(j),C(i)(j+1))
      }
    }
    C(X.length)(Y.length)
  }

  /**
   *  It will try to assign all elements from first to a close value by label from second
   * @return A sequence with the same amount of elements of first
   */
  def sortByComparableLabel[F <: ComparableByLabel, S <: ComparableByLabel](first: Seq[F], second: Seq[S]): Seq[(F,S)] = {
    assert(second.nonEmpty)
    first.map(f => {
      f -> second.maxBy(s => LCSLength(f.stringLabelNormalized, s.stringLabelNormalized))
    })
  }
}
