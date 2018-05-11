trait List[+T]

case class Nil() extends List[Nothing]
case class Cons[+T](head: T, tail: List[T]) extends List[T]

sealed class Car
sealed class Volvo extends Car

object Main extends App {
    val x: List[Car] = new Cons(new Volvo, new Nil())
    println(x)
}