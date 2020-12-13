import scala.quoted._

object Macros where
  implicit inline def identityMaped[T](x: => T): T = ${ MacrosImpl.impl('x) }

object MacrosImpl where
  def impl[T: Type](x: Expr[T])(using Quotes) : Expr[T] = {
    import quotes.reflect._
    val identityMap = new TreeMap { }
    val transformed = identityMap.transformTerm(x.asTerm)(Symbol.spliceOwner).asExprOf[T]
    transformed
  }
