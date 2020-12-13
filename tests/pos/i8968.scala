object Foo where
  inline def get = 0

object Bar where
  export Foo._

val v = Bar.get
