trait C where
  inline def x: Int
  inline val y: Int

class C1 extends C where
  inline val x = 1
  inline val y = 2

class C2 extends C where
  inline def x = 3
  inline val y = 4
