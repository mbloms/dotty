package a

import scala.quoted._


object A where

  inline def transform[A](inline expr: A): A = ${
    transformImplExpr('expr)
  }

  def pure[A](a:A):A = ???

  def transformImplExpr[A:Type](using Quotes)(expr: Expr[A]): Expr[A] = {
     import quotes.reflect._
     expr.asTerm match {
         case Inlined(x,y,z) => transformImplExpr(z.asExpr.asInstanceOf[Expr[A]])
         case r@Apply(fun,args) =>  '{
              A.pure(${r.asExpr.asInstanceOf[Expr[A]]}) }
         case other => expr
     }
  }
