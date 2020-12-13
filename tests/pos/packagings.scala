package foo where
  package bar where
    object A where
      def foo = 1
  end bar
end foo
package baz where
  object B where
    def f = foo.bar.A.foo
end baz
