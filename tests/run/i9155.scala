object Foo where
  @scala.annotation.targetName("w") def \/\/ = "W"

object Bar where
  export Foo._

@main def Test =
  assert(Foo.getClass.getMethods.exists(_.getName == "w"))
  assert(Bar.getClass.getMethods.exists(_.getName == "w"))
