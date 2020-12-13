package scala.quoted

trait FromExpr[T] where
  def unapply(x: Expr[T])(using Quotes): Option[T]
