package dotty.tools.dotc
package transform

import dotty.tools.dotc.ast.Trees._
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Contexts
import dotty.tools.dotc.core.Phases._
import dotty.tools.dotc.transform.MegaPhase._
import dotty.tools.dotc.core.Names._
import dotty.tools.dotc.core.Types._

class MkPhantom extends MiniPhase {

  override def phaseName: String = "MkPhantom"

  override def transformStats(trees: List[tpd.Tree])(implicit ctx: Contexts.Context): List[tpd.Tree] = {
    for (declaration <- trees;
         typeDef <- (declaration match {
           case TypeDef(name, tree) => List(TypeDef(name, tree), TypeDef((name ++ "Phantom").toTypeName, tree))
           case _ => List(declaration)
         }))
      yield typeDef
  }
  
  def phantomClass(classDef: tpd.Tree) = classDef match {
    case TypeDef(name, tree : tpd.Tree) => {
      val newName = (name ++ "Phantom").toTypeName
      val originalType = TypeTree[tree.tpe]
      //tree.tpe
      TypeDef(newName, Template(null, originalType :: Nil))
    }
  }
}
