class Bil(val x: Int)

class Volvo extends Bil(1)
class Saab(i: Int) extends Bil(i)

object Main {
    val b: Bil = new Bil(4)
}