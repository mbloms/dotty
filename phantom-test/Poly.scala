class Bil(val x: Int) {
    def this() = this(1)
}

class Volvo extends Bil(1)

object Main extends App {
    val b: Bil.Phantom = Volvo.newPhantom()
}