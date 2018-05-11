class Stream[T](head: T, tail: Stream[T])

object Main extends App {
    val x: Stream[Int] = new Stream(1, null)
    println(x)
}