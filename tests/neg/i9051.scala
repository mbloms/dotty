package zio where

  class ZRef
  object ZRef where

    private[zio] implicit class ZRefSyntax(private val self: ZRef):
      def unsafeUpdate: Boolean = true

object Main where
  val ref = new zio.ZRef
  println(ref.unsafeUpdate)  // error
