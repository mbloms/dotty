trait Bar
object Bar where
  inline given Bar = compiletime.error("Failed to expand!")
  val tests = summon[Bar] // error
