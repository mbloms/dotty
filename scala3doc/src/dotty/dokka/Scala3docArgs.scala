package dotty.dokka

import org.jetbrains.dokka._
import org.jetbrains.dokka.utilities._
import org.jetbrains.dokka.plugability._
import java.util.ServiceLoader
import java.io.File
import java.util.jar._
import collection.JavaConverters._
import collection.immutable.ArraySeq

import java.nio.file.Files

import dotty.tools.dotc.config.Settings._
import dotty.tools.dotc.config.CommonScalaSettings
import dotty.dokka.Scala3doc._

class Scala3docArgs extends SettingGroup with CommonScalaSettings where
  val unsupportedSettings = Seq(
    // Options that we like to support
    bootclasspath, extdirs, javabootclasspath, encoding, usejavacp,
    // Needed for plugin architecture
    plugin,disable,require, pluginsDir, pluginOptions,
    // we need support for sourcepath and sourceroot
    sourcepath, sourceroot
  )

  val sourceLinks: Setting[String] =
    StringSetting("-source-links", "sources", SourceLinks.usage, "")

  val syntax: Setting[String] =
    StringSetting("-comment-syntax", "syntax", "Syntax of the comment used", "")

  val revision: Setting[String] =
    StringSetting("-revision", "revision", "Revision (branch or ref) used to build project project", "")

  val externalDocumentationMappings: Setting[String] =
    StringSetting("-external-mappings", "external-mappings", "Mapping between regex matching class file and external documentation", "")

  def scala3docSpecificSettings: Set[Setting[_]] = Set(sourceLinks, syntax, revision, externalDocumentationMappings)

object Scala3docArgs where
  def extract(args: List[String], rootCtx: CompilerContext):(Scala3doc.Args, CompilerContext) =
    val inst = new Scala3docArgs
    import inst._
    val initialSummary =
      ArgsSummary(defaultState, args, errors = Nil, warnings = Nil)
    val summary =
      processArguments(initialSummary, processAll = true, skipped = Nil)
    val newContext = rootCtx.fresh

    extension[T](arg: Setting[T])
      def get = arg.valueIn(summary.sstate)
      def withDefault(default: => T) =
        if arg.get == arg.default then default else arg.get
      def nonDefault =
        if arg.get == arg.default then None else Some(arg.get)

    def setInGlobal[T](s: Setting[T]) =
      s.nonDefault.foreach { newValue =>
        newContext.settings.allSettings.find(_ == s).fold(
          report.warning(s"Unable to set ${s.name} in global context")
        )(s => newContext.setSetting(s.asInstanceOf[Setting[T]], newValue))
      }

    allSettings.filterNot(scala3docSpecificSettings.contains).foreach(setInGlobal)

    given CompilerContext = newContext
    summary.warnings.foreach(report.warning(_))
    summary.errors.foreach(report.error(_))

    def parseTastyRoots(roots: String) =
      roots.split(File.pathSeparatorChar).toList.map(new File(_))

    val inFiles = summary.arguments.map(File(_)).filter(_.getName != "___fake___.scala")
    val (existing, nonExisting) = inFiles.partition(_.exists)

    if nonExisting.nonEmpty then report.warning(
      s"Scala3doc will ignore following nonexisiten paths: ${nonExisting.mkString(", ")}"
    )

    val (dirs, files) = existing.partition(_.isDirectory)
    val (validFiles, other) = files.partition(f =>
      f.getName.endsWith(".tasty") || f.getName.endsWith(".jar")
    )

    if other.nonEmpty then report.warning(
      s"Scala3doc suports only .tasty and .jar files, following files will be ignored: ${other.mkString(", ")}"
    )

    def defaultDest(): File =
      report.error("Destenation is missing, please provide '-d' parameter pointing to directory here docs should be created")
      File("output")

    val parseSyntax = syntax.nonDefault.fold(CommentSyntax.default){ str =>
      CommentSyntax.parse(str).getOrElse{
        report.error(s"unrecognized value for -syntax option: $str")
        CommentSyntax.default
      }
    }
    val externalMappings = externalDocumentationMappings.get.split(":::").map(_.split("::").toList).toList

    unsupportedSettings.filter(s => s.get != s.default).foreach { s =>
      report.warning(s"Setting ${s.name} is currently not supported.")
    }
    val destFile = outputDir.nonDefault.fold(defaultDest())(_.file)
    val printableProjectName = projectName.nonDefault.fold("")("for " + _ )
    report.inform(
      s"Generating documenation $printableProjectName in $destFile")

    val docArgs = Args(
      projectName.withDefault("root"),
      dirs,
      validFiles,
      classpath.get,
      destFile,
      siteRoot.nonDefault,
      projectVersion.nonDefault,
      projectLogo.nonDefault,
      parseSyntax,
      sourceLinks.nonDefault.fold(Nil)(_.split(",").toList),
      revision.nonDefault,
      externalMappings
    )
    (docArgs, newContext)