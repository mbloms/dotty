package autoParamTupling

object t1 where
  val xs: List[(Int, Int)] = ???

  xs.map {
    case (x, y) => x + y
  }

  xs.map {
    (x, y) => x + y
  }

  xs.map(_ + _)

