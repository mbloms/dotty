package tests.functionDRI

import scala.annotation.targetName


class A
class B

def a(as: Seq[A]) = 1
def a(a: A) = 2
def b(b: B) = 3

// scala3doc right now is ignroing @targetName
// def b(a: A, b: B) = 4
// @targetName("b2") def b(a: A)(b: B) = 5

// @targetName("c_a") def c(p: Seq[A]) = 6
// @targetName("c_b") def c(p: Seq[B]) = 7

// scala3 doc right now is not differentiating between fields and nested classlikes
// class C(val b: Int):
//   trait b

class D where
  trait b where
    def b = 8
  def b = 9

class E where
  class F where
    def b = 10
  object F where
    def b = 11
