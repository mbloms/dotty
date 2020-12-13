package tests
package overrides

class A where
  def defInt: Int = 1

class B extends A where
  override def defInt: Int = 2

class C extends B where
  override def defInt: Int = 3
