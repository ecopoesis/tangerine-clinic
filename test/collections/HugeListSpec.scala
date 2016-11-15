package collections

import org.scalatest.FlatSpec

class HugeListSpec extends FlatSpec {

  "HugeList" should "start with size 0" in {
    val huge = HugeList[Long](10)
    assert(huge.size == 0)
  }

  it should "add and get one item" in {
    val huge = HugeList[Long](10)
    huge.add(1L)
    assert(huge.size == 1)
    assert(huge.get(0) == 1L)
  }

  it should "add and get one item with apply" in {
    val huge = HugeList[Long](10)
    huge.add(1L)
    assert(huge.size == 1)
    assert(huge(0) == 1L)
  }


  it should "add and get items across buckets" in {
    val huge = HugeList[Long](10)
    for(i <- 0 to 1000) {
      huge.add(i)
    }
    assert(huge.size == 1001)
    assert(huge(0) == 0)
    assert(huge(10) == 10)
    assert(huge(100) == 100)
    assert(huge(1000) == 1000)
  }
}
