object impl where
  case object UNone

import impl._

opaque type UOption[+A] = (A | UNone.type) // error: Cyclic Reference involving UOption
