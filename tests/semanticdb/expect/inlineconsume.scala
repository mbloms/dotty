package inlineconsume

import inlinedefs.FakePredef.assert

class Foo where
  def test = assert(3 > 2)
