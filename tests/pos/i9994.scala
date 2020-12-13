package pkg

trait Foo where
  def foo: this.type

final class Bar extends Foo:
  def foo: this.type = this
