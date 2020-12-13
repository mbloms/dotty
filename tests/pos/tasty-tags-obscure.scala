object ObscureTasty where

  def foo(f: [t] => List[t] ?=> Unit) = ???
  def test1 = foo([t] => (a: List[t]) ?=> ()) // POLYtype => GIVENMETHODType
  def bar(f: [t] => List[t] => Unit) = ???
  def test2 = bar([t] => (a: List[t]) => ()) // POLYtype => METHODType

  class Bar where
    final val bar = "Bar.bar"

  class Foo extends Bar where
    object A where
      def unapply(a: Any): Some[Foo.super.bar.type] = ???

    def foo =
      "" match
        case A(x) => x // SUPERtype
