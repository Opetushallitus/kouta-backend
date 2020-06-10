package fi.oph.kouta.mocks

import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid}

trait OrganisaatioServiceMock extends ServiceMocks {

  val NotFoundOrganisaatioResponse = s"""{ "numHits": 0, "organisaatiot": []}"""
  lazy val DefaultResponse = responseFromResource("organisaatio")

  protected def organisaatioServiceParams(oid: OrganisaatioOid, lakkautetut: Boolean = false) = Map(
    "oid" -> oid.s,
    "aktiiviset" -> "true",
    "suunnitellut" -> "true",
    "lakkautetut" -> lakkautetut.toString)

  def singleOidOrganisaatioResponse(oid: String) = s"""{ "numHits": 1, "organisaatiot": [{"oid": "$oid", "parentOidPath": "$oid/$RootOrganisaatioOid", "oppilaitostyyppi": "oppilaitostyyppi_21#1", "children" : []}]}"""

  def mockOrganisaatioResponse(oid: OrganisaatioOid, response: String = DefaultResponse, lakkautetut: Boolean = false): Unit =
    mockGet("organisaatio-service.organisaatio.hierarkia", organisaatioServiceParams(oid, lakkautetut), response)

  def mockOrganisaatioResponses(oids: OrganisaatioOid*): Unit = oids.foreach(mockOrganisaatioResponse(_))

  def mockSingleOrganisaatioResponses(organisaatioOids: OrganisaatioOid*): Unit = organisaatioOids.foreach { oid =>
    mockOrganisaatioResponse(oid, singleOidOrganisaatioResponse(oid.s))
  }

  def mockSingleOrganisaatioResponses(first: String, organisaatioOids: String*): Unit =
    mockSingleOrganisaatioResponses((organisaatioOids :+ first).map(OrganisaatioOid): _*)
}

object OrganisaatioServiceMock extends OrganisaatioServiceMock
