import scala.quoted._

class Test where
  given NilToExpr: ToExpr[Nil.type] with {
    def apply(xs: Nil.type)(using Quotes): Expr[Nil.type] =
      '{ Nil }
  }
