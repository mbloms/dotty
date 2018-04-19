case class Fordon(val maxSpeed: Int) {
    def this() = this(0)

    def faster = new PhantomFordon1(maxSpeed+1)
}

trait PhantomFordon extends Fordon

class PhantomFordon1(maxSpeed: Int) extends Fordon(maxSpeed) with PhantomFordon
class PhantomFordon2 extends Fordon() with PhantomFordon

object Inner extends App {
    val f: Fordon = new Fordon
    val f1: Fordon = new PhantomFordon1(2)
    val f2: Fordon = new PhantomFordon2
    val f3: Fordon = new PhantomBil1(3)
    //val f: Fordon = new Fordon
    println(f)
    println(f1)
    println(f2)
    println(f3)
    println(f3.faster)
}

class Bil(maxSpeed: Int) extends Fordon(maxSpeed)

trait PhantomBil extends Bil with PhantomFordon

class PhantomBil1(maxSpeed: Int) extends Bil(maxSpeed) with PhantomBil