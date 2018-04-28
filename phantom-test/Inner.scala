class Fordon(val maxSpeed: Int) {
    protected var x = 4
    def mutate(f: Fordon) = f.x += 1
    def get = x
    def this() = this(0)

    def faster = {
        {val constr = implicitly[Int => Fordon]; constr(maxSpeed+1)}
    }
}

object Fordon extends Phantom[Fordon] {
    sealed trait Phantom extends Fordon
    implicit def PhantomFordon1(maxSpeed: Int): Phantom = new Fordon(maxSpeed) with Phantom
    implicit def PhantomFordon2: Phantom = new Fordon() with Phantom
    //implicit def simple(x: Unit): Phantom = PhantomFordon2
    def apply[A](arg: A)(implicit constr: A => Phantom) = constr(arg)
    //def apply(): Phantom = new PhantomFordon2
    //def apply(i: Int): Phantom = new PhantomFordon1(i)
}

object Inner extends App {
    val f: Fordon = new Fordon
    val f1: Fordon = Fordon(2)
    val f2: Fordon = implicitly[Fordon]
    val constr = implicitly[Int => Bil.Phantom]
    val f3: Fordon = constr(3)
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

//object Phantom {
//    def phantom[A,B](arg: A) = implicitly[]
//}

class Bil(maxSpeed: Int) extends Fordon(maxSpeed)

object Bil extends Phantom[Bil] {
    sealed trait Phantom extends Bil with Fordon.Phantom
    implicit final class PhantomBil1(maxSpeed: Int) extends Bil(maxSpeed) with Phantom
    def apply(i: Int): Phantom = new PhantomBil1(i)   
}