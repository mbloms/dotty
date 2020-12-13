class C(val x: Int)

trait D where
   type T
   def trans(other: T): T

def f(x: Int)(using c: C) (y: Int) = x + c.x + y
def g(x: Int)(using d: D) (y: d.T): d.T = d.trans(y)

@main def Test =
  given C(1) with {}
  val x = f
  assert(x(2)(3) == 6)

  given D with
    type T = Int
    def trans(other: T) = 2 * other
  val y = g
  assert(y(2)(3) == 6)
