package tests

package implicitMembers

class OuterClass where
  class ImplicitMemberTarget

  object ImplicitMemberTarget where
    extension (a: ImplicitMemberTarget)
      def extensionFromCompanion: String =
        "ImplicitMemberTarget"

  // does not work
  extension (a: ImplicitMemberTarget)
    def extensionFromOuterClass: String =
      "ImplicitMemberTarget"

extension (a: OuterClass#ImplicitMemberTarget)
    def extensionFromPackage: String =
      "ImplicitMemberTarget"