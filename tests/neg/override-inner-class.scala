class C where
  type T >: String <: Any

class D extends C where
  class T  // error
