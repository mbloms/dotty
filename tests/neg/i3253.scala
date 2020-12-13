import Test.test

class A where
  def test = "  " * 10 // error
object Test extends A
