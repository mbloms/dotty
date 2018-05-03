package dotty.tools.dotc
package transform

import dotty.tools.dotc.ast.Trees.{Apply, New, Select}
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.ast.tpd.{TreeOps, ref}
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Flags.ModuleOrFinal
import dotty.tools.dotc.core.Mode.InSuperCall
import dotty.tools.dotc.core.Names.termName
import dotty.tools.dotc.transform.MegaPhase.MiniPhase

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

  override def transformSelect(tree: tpd.Select)(implicit ctx: Context): tpd.Tree = tree match {
    case Select(New(i),_) => {
      if (i.symbol.flags.is(ModuleOrFinal) || (ctx.mode.is(InSuperCall) && ctx.owner.isClassConstructor))
        tree
      else {
        println(tree.showSummary)
        ref(i.symbol.companionModule).select(newPhantom)
      }
    }
    case _ => tree
  }
}
