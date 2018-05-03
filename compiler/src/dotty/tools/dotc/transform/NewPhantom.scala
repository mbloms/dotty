package dotty.tools.dotc
package transform

import dotty.tools.dotc.ast.Trees._
import dotty.tools.dotc.ast.tpd._
import dotty.tools.dotc.ast.{Trees, tpd, untpd}
import dotty.tools.dotc.core.{Contexts, Names}
import dotty.tools.dotc.core.Mode.InSuperCall
import dotty.tools.dotc.core.Contexts._
import dotty.tools.dotc.core.Flags._
import dotty.tools.dotc.core.Names._
import dotty.tools.dotc.core.NameOps._
import dotty.tools.dotc.transform.MegaPhase.MiniPhase
import dotty.tools.dotc.util.Store

object NewPhantom {
  val newPhantom = termName("newPhantom")
}

class NewPhantom extends MiniPhase {
  import NewPhantom._
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


  override def transformApply(tree: tpd.Apply)(implicit ctx: Context): tpd.Tree = {
    println(tree)
    tree match {
      case Apply(s@Select(_, name), Nil) if name == newPhantom => {
        println(s)
        s
      }
      case _ => tree
    }
  }

  override def transformSelect(tree: tpd.Select)(implicit ctx: Context): tpd.Tree = tree match {
    case Select(New(i),_) => {
      if (i.symbol.flags.is(ModuleOrFinal) || (ctx.mode.is(InSuperCall) && ctx.owner.isClassConstructor))
        tree
      else
        ref(i.symbol.companionModule).select(newPhantom)
    }
    case _ => tree
  }

  override def transformUnit(tree: tpd.Tree)(implicit ctx: Context): tpd.Tree = {
    println(tree.showSummary)
    tree
  }
}
