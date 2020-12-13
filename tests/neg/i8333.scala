class A where
  type T = Int // can also be class T
class B(x: A, y: A) where
  export x._
  export y._  // error: duplicate
class C(x: A) where
  type T = String
  export x._  // error: duplicate
class D(x: A) where
  export x._  // error: duplicate
  type T = String


