package collections

import scala.collection.mutable

class HugeList[T](bucketSize: Int = Int.MaxValue) {
  var parent = scala.collection.mutable.Buffer[scala.collection.mutable.Buffer[T]]()

  def apply(i: Long): T = get(i)

  def get(i: Long): T = {
    val bucket = i / bucketSize
    if (bucket > parent.size - 1) {
      throw new IndexOutOfBoundsException
    }
    parent(bucket.toInt)((i - (bucket * bucketSize)).toInt)
  }

  def size: Long = {
    if (parent.isEmpty) {
      0
    } else {
      ((parent.size - 1) * bucketSize) + parent.last.size
    }
  }

  def add(o: T) = {
    if (parent.isEmpty || parent.last.size == bucketSize) {
      parent += mutable.Buffer[T]()
    }
    parent.last += o
  }
}

object HugeList {
  def apply[T](bucketSize: Int = Int.MaxValue) = new HugeList[T](bucketSize)
}
