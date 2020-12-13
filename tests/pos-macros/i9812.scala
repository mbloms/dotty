// compile with -Ycheck:reifyQuotes -Ystop-after:reifyQuotes
import quoted._

sealed abstract class SomeEnum
object SomeEnum where
  final val Foo = new SomeEnum {}

def quoteFoo: Quotes ?=> Expr[SomeEnum.Foo.type] = '{SomeEnum.Foo}
