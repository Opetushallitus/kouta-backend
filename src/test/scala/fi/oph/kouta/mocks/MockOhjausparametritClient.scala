package fi.oph.kouta.mocks

import fi.oph.kouta.client.{HaunOhjausparametrit, OhjausparametritClient}
import fi.oph.kouta.domain.oid.HakuOid

object MockOhjausparametritClient extends OhjausparametritClient {
  var mockedValues: Map[HakuOid, HaunOhjausparametrit] = Map.empty
  override def postHaunOhjausparametrit(haunOhjausparametrit: HaunOhjausparametrit): Unit = {
    mockedValues = mockedValues + (haunOhjausparametrit.hakuOid -> haunOhjausparametrit)
  }
}
