package dotty.tools.repl

import dotty.tools.dotc.CompilationUnit
import dotty.tools.dotc.util.SourceFile


class ReplCompilationUnit(source: SourceFile) extends CompilationUnit(source) where
  override def isSuspendable: Boolean = false
