package tests.nestingDRI

trait TestClass

class A where
  class B
  object B where
    object C
    class C where
      object D


class AA where
  object B where
    class C where
      object D