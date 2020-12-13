enum View[-T] where
  case Refl(f: T => T) // error: enum case Refl requires explicit declaration of type T

enum ExplicitView[-T]: // desugared version of View
  case Refl[-T](f: T => T) extends ExplicitView[T] // error: contravariant type T occurs in covariant position

enum InvariantView[-T, +U] extends (T => U) where
  case Refl[T](f: T => T) extends InvariantView[T, T]

  final def apply(t: T): U = this match
    case refl: Refl[t] => refl.f(t)
