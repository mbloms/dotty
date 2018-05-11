trait List[T]

class Nil() extends List[Nothing]
class Cons[T](head: T, tail: List[T]) extends List[T]