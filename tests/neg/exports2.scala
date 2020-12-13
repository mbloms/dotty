object A where
  def f: String = ""

trait B where
  def f: String = "abc"

object C extends B where
  export A._            // error

