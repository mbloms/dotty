package hello

enum Color where
  case Red, Green, Blue

object HelloWorld where
  def main(args: Array[String]): Unit = {
    println("hello dotty.superbootstrapped!")
    println(Color.Red)
    println(Color.Green)
    println(Color.Blue)
  }
