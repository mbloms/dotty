class Bil(val x: Int) {
    def this() = this(1)
}

class Volvo extends Bil(1)
final class Saab(i: Int) extends Bil(i)

object Main extends App {
    val b: Bil = new Bil(4)
    println(b)
    println(new Bil)
    println(new Bil(2))
    println(new Saab(2))
}