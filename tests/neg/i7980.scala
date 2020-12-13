trait Evidence[X]

trait Trait[X : Evidence] where
  def method(x : X) : X

given ev: Evidence[Int] = new Evidence[Int]{}
val crash : Trait[Int] = (x: Int) => x          // error
