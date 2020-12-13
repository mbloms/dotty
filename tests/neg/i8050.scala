object stuff where
  def exec(dir: Int) = ???

extension (a: Int)
  inline def exec: Unit = stuff.exec("aaa") // error
