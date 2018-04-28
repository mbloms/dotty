class Bil(val x: Int)

class Volvo extends Bil(1)
final class Saab(i: Int) extends Bil(i)

object Main extends App {
    val b: Bil = new Bil(4) {}
    println(b)
}