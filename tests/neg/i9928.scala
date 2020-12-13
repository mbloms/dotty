trait Magic[F] where
  extension (x: Int) def read: F

object Magic where
  given Magic[String] with
    extension(x: Int) def read: String =
      println("In string")
      s"$x"

opaque type Foo = String
object Foo where
  import Magic.given
  def apply(s: String): Foo = s

  given Magic[Foo] with
    extension (x: Int) def read: Foo =
      println("In foo")
      Foo(s"$x")

  def test: Unit =
    (3.read: Foo)  // error: ambiguous


@main def Test = {
  Foo.test
}