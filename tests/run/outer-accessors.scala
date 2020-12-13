class A where
  val a = 2

  class B where
    val b = 3

    trait T where
      def t = a + b

  val bb = B()

  class C extends bb.T where
    def result = a + t

@main def Test =
  val a = A()
  val c = a.C()
  assert(c.result == 7)
