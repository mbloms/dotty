package dotty.tools.dotc
package transform

import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Contexts
import dotty.tools.dotc.core.Phases._
import dotty.tools.dotc.transform.MegaPhase.MiniPhase

class MkPhantom extends MiniPhase{
  override def transformStats(trees: List[tpd.Tree])(implicit ctx: Contexts.Context): List[tpd.Tree] =
    for (declaration <- trees)
      yield declaration match {
        case 
      }
}
