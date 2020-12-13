package scala.quoted

package object staging where

  /** Evaluate the contents of this expression and return the result.
   *  It provides a new Quotes that is only valid within the scope the argument.
   *
   *  Usage:
   *  ```
   *  val e: T = run { // (quotes: Quotes) ?=>
   *    expr
   *  }
   *  ```
   *  where `expr: Expr[T]`
   *
   *  This method should not be called in a context where there is already has a `Quotes`
   *  such as within a `run` or a `withQuotes`.
   */
  def run[T](expr: Quotes ?=> Expr[T])(using toolbox: Toolbox): T = toolbox.run(expr(using _))

  /** Provide a new quote context within the scope of the argument that is only valid within the scope the argument.
   *  Return the result of the argument.
   *
   *  Usage:
   *  ```
   *  val e: T = withQuotes { // (quotes: Quotes) ?=>
   *    thunk
   *  }
   *  ```
   *  where `thunk: T`
   *
   *  This method should not be called in a context where there is already has a `Quotes`
   *  such as within a `run` or a `withQuotes`.
   */
  def withQuotes[T](thunk: Quotes ?=> T)(using toolbox: Toolbox): T =
    val noResult = new Object
    var result: T = noResult.asInstanceOf[T]
    def dummyRun(using Quotes): Expr[Unit] = {
      result = thunk
      '{}
    }
    toolbox.run(dummyRun(using _))
    assert(result != noResult) // toolbox.run should have thrown an exception
    result
  end withQuotes

end staging
