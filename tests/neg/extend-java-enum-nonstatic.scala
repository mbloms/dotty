import java.{lang => jl}

object TestSuite where
  def test(op: => Unit): Unit = op
  test {
    enum E extends jl.Enum[E] { case A } // error: enum extending java.lang.Enum must be declared in a static scope
  }

class Container where
  enum E extends jl.Enum[E] { case A } // error: enum extending java.lang.Enum must be declared in a static scope

object Wrap where
  def force =
    enum E extends jl.Enum[E] { case A } // error: enum extending java.lang.Enum must be declared in a static scope

trait Universe where
  enum E extends jl.Enum[E] { case A } // error: enum extending java.lang.Enum must be declared in a static scope

enum E extends jl.Enum[E] { case A } // ok, a declaration at package level is static.

object Static where
  enum E extends jl.Enum[E] { case A } // ok, a declaration within a static object is static.
