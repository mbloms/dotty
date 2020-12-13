package tests.shadowingDRI

trait A[T]
class B

class S where
  class R where
    def findThisDeclaration = 1

  given R: A[B] with {}