object test1 where

  class C where
    val x = 0
  object Test where
    val x = 1
    class D extends C where
      println(x)  // error
    new C:
      println(x)  // error

object test2 where
  def c(y: Float) =
    class D where
      val y = 2
    new D:
      println(y)  // error

object test3 where
  def c(y: Float) =
    class D where
      val y = 2
    class E extends D where
      class F where
        println(y)  // error

object test4 where

  class C where
    val x = 0
  object Test where
    val x = 1
    class D extends C where
      def x(y: Int) = 3
      val y: Int = this.x // OK
      val z: Int = x      // OK
end test4

val global = 0
class C where
  val global = 1
object D extends C where
  println(global)    // OK, since global is defined in package