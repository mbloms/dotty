trait Show[A] where
  def show(a: A): String = a.toString

object Prefix where
  type AbstractType
  type UpperBoundedType <: String
  type FullyBoundedType >: String <: String

  given A: Show[AbstractType] with {}
  given B: Show[UpperBoundedType] with {}
  given C: Show[FullyBoundedType] with {}

@main def Test =
  summon[Show[Prefix.AbstractType]]
  summon[Show[Prefix.UpperBoundedType]]
  summon[Show[Prefix.FullyBoundedType]]
