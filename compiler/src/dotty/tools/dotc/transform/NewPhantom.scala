package dotty.tools.dotc
package transform

import dotty.tools.dotc.ast.Trees._
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Contexts
import dotty.tools.dotc.core.Mode.InSuperCall
import dotty.tools.dotc.core.Contexts._
import dotty.tools.dotc.core.Phases._
import dotty.tools.dotc.transform.MegaPhase.MiniPhase
import dotty.tools.dotc.util.Store

class NewPhantom extends MiniPhase {
  /** A name given to the `Phase` that can be used to debug the compiler. For
    * instance, it is possible to print trees after a given phase using:
    *
    * ```bash
    * $ ./bin/dotc -Xprint:<phaseNameHere> sourceFile.scala
    * ```
    */
  override def phaseName: String = "NewPhantom"

  private var Blacklist: Store.Location[Boolean] = _
  private def blacklist(implicit ctx: Context): Boolean = ctx.store(Blacklist)

  override def initContext(ctx: FreshContext) =
    Blacklist = ctx.addLocation(true)

  private def setBlacklist(p: Boolean)(implicit ctx: Context) =
    if (blacklist == p) ctx else ctx.fresh.updateStore(Blacklist, p)


  override def prepareForStats(trees: List[tpd.Tree])(implicit ctx: Context): Context = {
    setBlacklist(false)
  }


  override def prepareForTemplate(tree: tpd.Template)(implicit ctx: Context): Context = {
    setBlacklist(true)
  }

  override def transformNew(tree: tpd.New)(implicit ctx: Contexts.Context): tpd.Tree = {
    println(ctx.mode.is(InSuperCall))
    println(tree.showSummary)
    tree
  }
}
