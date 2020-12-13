import annotation.targetName
class A where
  def f(): Int = 1
class B extends A:   // error
  @targetName("f") def g(): Int = 2

