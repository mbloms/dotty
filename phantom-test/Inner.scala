class Fordon(val maxSpeed: Int) {
    protected var x = 4
    def mutate(f: Fordon) = f.x += 1
    def get = x
    def this() = this(0)

    def faster(implicit ev: Phantom[Fordon]) = Fordon(maxSpeed+1)
}

object Fordon extends Phantom[Fordon] {
    sealed trait Phantom extends Fordon
    private final class PhantomFordon1(maxSpeed: Int) extends Fordon(maxSpeed) with Phantom
    private final class PhantomFordon2 extends Fordon() with Phantom
    def apply(): Phantom = new PhantomFordon2
    def apply(i: Int): Phantom = new PhantomFordon1(i)
}

object Inner extends App {
    val f: Fordon = new Fordon
    val f1: Fordon = Fordon(2)
    val f2: Fordon = Fordon()
    val f3: Fordon = Bil(3)
    //val f: Fordon = new Fordon
    println(f)
    println(f1)
    println(f2)
    println(f3)
    println(f3.faster)
    f1.mutate(f1)
    println(f1.get)
    f1.mutate(f1)
    println(f1.get)
}

trait Phantom[T]

object Phantom {
    implicit def fordon: Phantom[Fordon] = Fordon
    implicit def bil: Phantom[Bil] = Bil
}

class Bil(maxSpeed: Int) extends Fordon(maxSpeed)

object Bil extends Phantom[Bil] {
    sealed trait Phantom extends Bil with Fordon.Phantom
    private final class PhantomBil1(maxSpeed: Int) extends Bil(maxSpeed) with Phantom
    def apply(i: Int): Phantom = new PhantomBil1(i)   
}