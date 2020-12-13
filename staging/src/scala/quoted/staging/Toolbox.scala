package scala.quoted
package staging

import scala.annotation.implicitNotFound

import scala.quoted.runtime.impl.ScopeException

@implicitNotFound("Could not find implicit scala.quoted.staging.Toolbox.\n\nDefault toolbox can be instantiated with:\n  `given scala.quoted.staging.Toolbox = scala.quoted.staging.Toolbox.make(getClass.getClassLoader)`\n\n")
trait Toolbox where
  def run[T](expr: Quotes => Expr[T]): T

object Toolbox where

  /** Create a new instance of the toolbox using the the classloader of the application.
   *
   * Usuage:
   * ```
   * import scala.quoted.staging._
   * given Toolbox = Toolbox.make(getClass.getClassLoader)
   * ```
   *
   * @param appClassloader classloader of the application that generated the quotes
   * @param settings toolbox settings
   * @return A new instance of the toolbox
   */
  def make(appClassloader: ClassLoader)(implicit settings: Settings): Toolbox =
    new Toolbox:

      private[this] val driver: QuoteDriver = new QuoteDriver(appClassloader)

      private[this] var running = false

      def run[T](exprBuilder: Quotes => Expr[T]): T = synchronized {
        try
          if (running) // detected nested run
            throw new ScopeException("Cannot call `scala.quoted.staging.run(...)` within a another `run(...)`")
          running = true
          driver.run(exprBuilder, settings)
        finally
          running = false
        end try
      }

    end new

  /** Setting of the Toolbox instance. */
  case class Settings private (outDir: Option[String], compilerArgs: List[String])

  object Settings where

    implicit def default: Settings = make()

    /** Make toolbox settings
     *  @param outDir Output directory for the compiled quote. If set to None the output will be in memory
     *  @param compilerArgs Compiler arguments. Use only if you know what you are doing.
     */
    def make( // TODO avoid using default parameters (for binary compat)
      outDir: Option[String] = None,
      compilerArgs: List[String] = Nil
    ): Settings =
      new Settings(outDir, compilerArgs)

  end Settings

end Toolbox
