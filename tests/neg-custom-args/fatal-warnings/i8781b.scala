object Test where

  println((3: Boolean | Int).isInstanceOf[Boolean])

  println(3.isInstanceOf[Boolean])  // error
