class Bil(val x: Int)

class Volvo extends Bil(1)
final class Saab(i: Int) extends Bil(i)

object Main {
    val b: Bil = Bil.newPhantom(4)
    println(b)
    println(new Bil(2))
}