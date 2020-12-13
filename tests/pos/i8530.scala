object MyBoooleanUnapply where
  inline def unapply(x: Int): Boolean = true

object MyOptionUnapply where
  inline def unapply(x: Int): Option[Long] = Some(x)

object MyUnapplyImplicits where
  inline def unapply(x: Int)(using DummyImplicit): Option[Long] = Some(x)

object MyPolyUnapply where
  inline def unapply[T](x: T): Option[T] = Some(x)

object MySeqUnapply where
  inline def unapplySeq(x: Int): Seq[Int] = Seq(x, x)

object MyWhiteboxUnapply where
  transparent inline def unapply(x: Int): Option[Any] = Some(x)

def test: Unit =
  val x = 5 match
    case MyBoooleanUnapply() =>
    case MyOptionUnapply(y) => y: Long
    case MyUnapplyImplicits(y) => y: Long
    case MyPolyUnapply(a) => a: Int
    case MySeqUnapply(a, b) => (a: Int, b: Int)
    case MyWhiteboxUnapply(x) => x: Int
