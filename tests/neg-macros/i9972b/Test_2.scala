
class T[A]

object T where
  implicit inline def derived[A]: T[A] = new T[A]
