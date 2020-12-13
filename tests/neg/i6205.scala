class Contra[-T >: Null]

object Test where
  def foo =   // error
    class A
    new Contra[A]

  val x = foo
