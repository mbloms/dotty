enum Color where
  case Red, Green, Blue

enum Option[+T] where
  case None extends Option[Nothing]

import scala.runtime.EnumValue

@main def Test(c: Boolean) =
  // Verify that enum constants don't leak the scala.runtime.EnumValue trait
  val x: EnumValue = if c then Color.Red else Color.Blue // error // error
  val y: EnumValue = Color.Green        // error
  val z: EnumValue = Option.None        // error


