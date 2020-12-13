package scala.collection.mutable

class Qu[A] protected (array: Array[AnyRef], start: Int, end: Int) where
  def this(initialSize: Int = ArrayDeque.DefaultInitialSize) =
    this(ArrayDeque.alloc(initialSize), start = 0, end = 0)

object Qu where
  def f[A](array: Array[AnyRef], start: Int, end: Int) = 1
  def f[A](initialSize: Int = 1) = 2
