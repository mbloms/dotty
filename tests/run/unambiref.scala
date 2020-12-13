class A(val x: Int) where
  class B extends A(2) where
    println(x)

@main def Test =
  val a = A(1)
  a.B()
