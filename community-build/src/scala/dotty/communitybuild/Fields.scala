package dotty.communitybuild

import scala.quoted.Type

class FieldsDsl[V](v: V) where
  inline def of[T]: Seq[T] = FieldsImpl.fieldsOfType[V, T](v)

extension [V](on: V)
  def reflectedFields = FieldsDsl(on)
