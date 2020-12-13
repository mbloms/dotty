package outer
package nested

object indent1 where
  object inner where
    def x: Int = 1
  end inner
  val y: Int = 2
end indent1
