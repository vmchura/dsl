package shared.utils

import org.scalatest._
import flatspec._
import matchers._

class UtilTest extends AnyFlatSpec with should.Matchers {

  "A LCS" should "give correct answer: test 1" in {
    val a = new ComparableByLabel {
      override def stringLabel: String = "ABCDE"
    }
    val b = new ComparableByLabel {
      override def stringLabel: String = "ACEXYZ"
    }

    assertResult(3)(Util.LCSLength(a.stringLabel,b.stringLabel))
  }
  it should "give correct answer: test 2" in {
    val a = new ComparableByLabel {
      override def stringLabel: String = "MZJAWXU"
    }
    val b = new ComparableByLabel {
      override def stringLabel: String = "XMJYAUZ"
    }

    assertResult(4)(Util.LCSLength(a.stringLabel,b.stringLabel))
  }

  "A sort by comparable test" should "give correct matching" in {
    case class Labeled(stringLabel: String) extends ComparableByLabel
    val first = List("holaholaz","1230","hoola","1239y46").map(s => Labeled(s))
    val second = List("holaz","hooLA","30").map(s => Labeled(s))
    val result = List("holaholaz","1230","hoola","1239y46").zip(List("holaz","30","hooLA","30")).map{case (x,y) => (Labeled(x), Labeled(y))}
    result.zip(Util.sortByComparableLabel(first,second)).foreach{case (x,y)  => assertResult(x)(y)}
  }


}