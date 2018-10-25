package fi.oph.kouta.integration

import java.util.UUID

package object fixture {

  case class Oid(oid:String)
  case class Id(id:UUID)
  case class Updated(updated:Boolean)

}
