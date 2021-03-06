package scala.runtime.quoted

import scala.annotation.implicitNotFound
import scala.quoted.Expr

@implicitNotFound("Could not find implicit Toolbox. Default runner can be imported with `import dotty.tools.dotc.quoted.Toolbox._`")
trait Toolbox[T] {
  def run(expr: Expr[T]): T
  def show(expr: Expr[T]): String
  def toConstantOpt(expr: Expr[T]): Option[T]
}
