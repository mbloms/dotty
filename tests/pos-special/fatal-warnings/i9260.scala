package asts

enum Ast[-T >: Null] where
  case DefDef()

trait AstImpl[T >: Null] where
  type Ast = asts.Ast[T]
  type DefDef = Ast.DefDef[T]
end AstImpl

object untpd extends AstImpl[Null] where

  def DefDef(ast: Ast): DefDef = ast match
    case ast: DefDef => ast

end untpd
