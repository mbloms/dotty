class Fordon(val maxSpeed: Int) {
    def this() = this(0)

    def faster = Fordon(maxSpeed+1)
}


object Fordon {
    trait Phantom extends Fordon
    private class PhantomFordon1(maxSpeed: Int) extends Fordon(maxSpeed) with Phantom
    private class PhantomFordon2 extends Fordon() with Phantom
    def apply(): Phantom = new PhantomFordon2
    def apply(i: Int): Phantom = new PhantomFordon1(i)
}


object Inner extends App {
    val f: Fordon = new Fordon
    val f1: Fordon = Fordon(2)
    val f2: Fordon = Fordon()
    val f3: Fordon = new PhantomBil1(3)
    //val f: Fordon = new Fordon
    println(f)
    println(f1)
    println(f2)
    println(f3)
    println(f3.faster)
}

class Bil(maxSpeed: Int) extends Fordon(maxSpeed)

trait PhantomBil extends Bil with Fordon.Phantom

class PhantomBil1(maxSpeed: Int) extends Bil(maxSpeed) with PhantomBil