class Bil(val x: Int) {
    def this() = this(1)
    def getAudi(regnr: String) = Audi(regnr)
    def copy = new Bil(x)
}

class Volvo extends Bil(1) {
    override def copy = new Volvo
}

final class Saab(i: Int) extends Bil(i) {
    override def copy = new Saab(i)
}

case class Audi(regnr: String) extends Bil(3)