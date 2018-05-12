object Main extends App {
    val b: Bil = new Volvo
    println(b)
    val a = new Bil
    println(a)
    println(a.copy)
    val s = new Bil(2)
    println(s)
    println(s.copy)
    val d = new Saab(2)
    println(d)
    println(d.copy)

    val f = Audi("ABC 123")
    println(f)
    println(f.getClass)
}