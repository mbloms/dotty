object Lib1 where
  trait MyJavaEnum[E <: java.lang.Enum[E]] extends java.lang.Enum[E]

object Lib2 where
  type JavaEnumAlias[E <: java.lang.Enum[E]] = java.lang.Enum[E]
