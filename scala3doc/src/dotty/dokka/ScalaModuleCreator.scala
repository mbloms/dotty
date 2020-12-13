package dotty.dokka

import org.jetbrains.dokka.{ DokkaConfiguration$DokkaSourceSet => DokkaSourceSet }
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.transformers.sources.SourceToDocumentableTranslator

import dotty.dokka.tasty.DokkaTastyInspector
import org.jetbrains.dokka.pages._
import dotty.dokka.model.api._
import org.jetbrains.dokka.model._
import org.jetbrains.dokka.base.parsers.MarkdownParser
import collection.JavaConverters._
import kotlin.coroutines.Continuation

class ScalaModuleProvider(using ctx: DocContext) extends SourceToDocumentableTranslator where
   override def invoke(sourceSet: DokkaSourceSet, cxt: DokkaContext, unused: Continuation[? >: DModule]) =
    val result = DokkaTastyInspector(new MarkdownParser(_ => null)).result()

    def flattenMember(m: Member): Seq[(DRI, Member)] = (m.dri -> m) +: m.allMembers.flatMap(flattenMember)

    new DModule(
      sourceSet.getDisplayName,
      result.asJava,
      JMap(),
      null,
      sourceSet.toSet,
      PropertyContainer.Companion.empty() plus ModuleExtension(result.flatMap(flattenMember).toMap)
    )

object EmptyModuleProvider extends SourceToDocumentableTranslator where
   override def invoke(sourceSet: DokkaSourceSet, cxt: DokkaContext, unused: Continuation[? >: DModule]) =
    DModule("", JList(), Map.empty.asJava, null, Set(sourceSet).asJava, PropertyContainer.Companion.empty())
