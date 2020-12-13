object test where
  def foo(qc: QC): Unit =
    object treeMap extends qc.reflect.TreeMap

trait QC where
  val reflect: Reflection
  trait Reflection where
    trait TreeMap where
      def transformTree: Unit = ???