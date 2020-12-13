package dotty.tools
package dotc
package reporting

import core.Contexts._
import config.Config
import config.Printers
import core.Mode

/** This module is carefully optimized to give zero overhead if Config.tracingEnabled
 *  is false. The `trace` operation is called in various hotspots, so every tiny bit
 *  of overhead is unacceptable: boxing, closures, additional method calls are all out.
 */
object trace where

  inline def onDebug[TD](inline question: String)(inline op: TD)(using Context): TD =
    conditionally(ctx.settings.YdebugTrace.value, question, false)(op)

  inline def conditionally[TC](inline cond: Boolean, inline question: String, inline show: Boolean)(inline op: TC)(using Context): TC =
    if Config.tracingEnabled then
      apply(question, if cond then Printers.default else Printers.noPrinter, show)(op)
    else op

  inline def apply[T](inline question: String, inline printer: Printers.Printer, inline showOp: Any => String)(inline op: T)(using Context): T =
    if Config.tracingEnabled then
      doTrace[T](question, printer, showOp)(op)
    else op

  inline def apply[T](inline question: String, inline printer: Printers.Printer, inline show: Boolean)(inline op: T)(using Context): T =
    if Config.tracingEnabled then
      doTrace[T](question, printer, if show then showShowable(_) else alwaysToString)(op)
    else op

  inline def apply[T](inline question: String, inline printer: Printers.Printer)(inline op: T)(using Context): T =
    apply[T](question, printer, false)(op)

  inline def apply[T](inline question: String, inline show: Boolean)(inline op: T)(using Context): T =
    apply[T](question, Printers.default, show)(op)

  inline def apply[T](inline question: String)(inline op: T)(using Context): T =
    apply[T](question, false)(op)

  private def showShowable(x: Any)(using Context) = x match
    case x: printing.Showable => x.show
    case _ => String.valueOf(x)

  private val alwaysToString = (x: Any) => String.valueOf(x)

  private def doTrace[T](question: => String,
                         printer: Printers.Printer = Printers.default,
                         showOp: Any => String = alwaysToString)
                        (op: => T)(using Context): T =
    if ctx.mode.is(Mode.Printing) || (printer eq Printers.noPrinter) then op
    else
      // Avoid evaluating question multiple time, since each evaluation
      // may cause some extra logging output.
      val q = question
      val leading = s"==> $q?"
      val trailing = (res: Any) => s"<== $q = ${showOp(res)}"
      var finalized = false
      var logctx = ctx
      while logctx.reporter.isInstanceOf[StoreReporter] do logctx = logctx.outer
      def margin = ctx.base.indentTab * ctx.base.indent
      def finalize(result: Any, note: String) =
        if !finalized then
          ctx.base.indent -= 1
          report.log(s"$margin${trailing(result)}$note")
          finalized = true
      try
        report.log(s"$margin$leading")
        ctx.base.indent += 1
        val res = op
        finalize(res, "")
        res
      catch case ex: Throwable =>
        finalize("<missing>", s" (with exception $ex)")
        throw ex
end trace
