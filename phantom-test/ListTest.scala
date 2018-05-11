trait List[+T]

class Nil() extends List[Nothing]
class Cons[+T](head: T, tail: List[T]) extends List[T]

sealed class Car
sealed class Volvo extends Car

object Main extends App {
    val x: List[Car] = new Cons(new Volvo, new Nil())
    println(x)
}