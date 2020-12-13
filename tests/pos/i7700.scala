package test

trait Show[-A] where
  def show(a: A): String

object Macros where
  extension (sc: StringContext) inline def show(args: =>Any*): String = ???

object Show where
  extension [A] (a: A) def show(using S: Show[A]): String = S.show(a)

  export Macros.show