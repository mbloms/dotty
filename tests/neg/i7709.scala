
object X where
  protected class Y
object A where
  class B extends X.Y // error
  class B2 extends X.Y: // error
    def this(n: Int) = this()
  class B3(x: Any)
  class B4 extends B3(new X.Y) // error
  class B5(x: String) where
    def this(n: Int) = this(new X.Y().toString) // error
trait T where
  class B extends X.Y // error
class XX where
  protected class Y
class C where
  def xx = new XX
  def y  = new xx.Y  // error
class D where
  def this(n: Int) = {
    this()
    def xx = new XX
    def y  = new xx.Y  // error
  }
class YY extends XX where
  def y = new Y

package p where
  object X where
    protected class Y
  class Q extends X.Y // error
