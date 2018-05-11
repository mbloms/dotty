class Stream[-T](head: T, tail: Stream[T])

class Car
class Volvo extends Car

object Main extends App {
    val x: Stream[Volvo] = new Stream(new Car , null)
    println(x)
}