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

  override def transformApply(tree: tpd.Apply)(implicit ctx: Context): tpd.Tree = tree match {
    case Apply(Select(New(i),_),params) => {
      if (i.symbol.flags.is(ModuleOrFinal) || (ctx.mode.is(InSuperCall) && ctx.owner.isClassConstructor))
        tree
      else {
        println(tree)
        val res = ref(i.symbol.companionModule).select(newPhantom)
        if (params.isEmpty) res else {
          cpy.Apply(tree)(res,params)
        }
      }
    }
    case _ => tree
  }
}
