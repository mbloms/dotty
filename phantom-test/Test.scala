class Hej1(x: Int, y: Int)
class Hej2(val x: Int)
class Hej3(x: Int) extends Hej2(x)

object Main extends App {
    val a = new Hej1(1,2)
    val b = new Hej2(2)
    val c = new Hej3(3)
    println(a)
    println(b)
    println(c)
}