package dotty.tools.dotc
package transform

import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Contexts
import dotty.tools.dotc.core.Phases._
import dotty.tools.dotc.transform.MegaPhase.MiniPhase

class Dumper extends MiniPhase {
  /** A name given to the `Phase` that can be used to debug the compiler. For
    * instance, it is possible to print trees after a given phase using:
    *
    * ```bash
    * $ ./bin/dotc -Xprint:<phaseNameHere> sourceFile.scala
    * ```
    */
  override def phaseName: String = "Dumper"

  /*override def transformPackageDef(tree: tpd.PackageDef)(implicit ctx: Contexts.Context): tpd.Tree = {
    println(tree)
    tree
  }*/

  override def transformStats(trees: List[tpd.Tree])(implicit ctx: Contexts.Context): List[tpd.Tree] = {
    trees.map(println(_))
    trees.map(t => println(t.tpe))
    trees
  }

  /*override def transformTemplate(tree: tpd.Template)(implicit ctx: Contexts.Context): tpd.Tree = {
    
  }*/
}
