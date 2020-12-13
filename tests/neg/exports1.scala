object A where
  def f: String = ""

trait B where
  def f: String = "abc"

trait B2 extends B where
  override def f: String = "abc"

object D extends B where
  object b extends B
  export b._             // ok

object D1 extends B where
  object b extends B
  export b.f             // error

object D2 extends B where
  object b2 extends B2
  export b2.f            // error
