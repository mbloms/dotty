trait A where
  type B

enum Test where
  case Test(a: A, b: a.B)   // error: Implementation restriction: case classes cannot have dependencies between parameters

case class Test2(a: A, b: a.B) // error: Implementation restriction: case classes cannot have dependencies between parameters

