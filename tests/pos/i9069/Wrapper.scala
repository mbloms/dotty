sealed trait Foo
object Foo where
  case object Baz extends Foo
case class Bar(x: Int) extends Foo
