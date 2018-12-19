package fi.oph.kouta.integration

import java.util.UUID

import fi.oph.kouta.TestData.MinValintaperuste
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.integration.fixture.ValintaperusteFixture
import fi.oph.kouta.validation.Validations

class ValintaperusteSpec extends KoutaIntegrationSpec with ValintaperusteFixture with Validations {

  it should "return 404 if valintaperuste not found" in {
    get(s"/valintaperuste/${UUID.randomUUID()}") {
      status should equal (404)
      body should include ("Unknown valintaperuste id")
    }
  }

  it should "store valintaperuste" in {
    val id = put(valintaperuste)
    get(id, valintaperuste(id))
  }

  it should "update valintaperuste" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(valintaperuste(id, Arkistoitu), lastModified)
    get(id, valintaperuste(id, Arkistoitu))
  }

  it should "not update valintaperuste" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(valintaperuste(id), lastModified, false)
    get(id, valintaperuste(id)) should equal (lastModified)
  }

  it should "fail update if 'If-Unmodified-Since' header is missing" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    post(ValintaperustePath, bytes(valintaperuste(id))) {
      status should equal (400)
      body should include ("If-Unmodified-Since")
    }
  }

  it should "fail update if modified in between get and update" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    Thread.sleep(1500)
    update(valintaperuste(id, Arkistoitu), lastModified)
    post(ValintaperustePath, bytes(valintaperuste(id)), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "store and update unfinished valintaperuste" in {
    val unfinishedValintaperuste = MinValintaperuste
    val id = put(unfinishedValintaperuste)
    val lastModified = get(id, unfinishedValintaperuste.copy(id = Some(id)))
    val newUnfinishedValintaperuste = unfinishedValintaperuste.copy(id = Some(id), organisaatioOid = OrganisaatioOid("6.6.6.6.6"))
    update(newUnfinishedValintaperuste, lastModified)
    get(id, newUnfinishedValintaperuste)
  }

  it should "validate new valintaperuste" in {
    put(ValintaperustePath, bytes(valintaperuste.copy(organisaatioOid = OrganisaatioOid("saippua"))), List(jsonHeader)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validateErrorBody(validationMsg("saippua")))
    }
  }

  it should "validate updated valintaperuste" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    post(ValintaperustePath, bytes(valintaperuste(id).copy(organisaatioOid = OrganisaatioOid("saippua"))), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validateErrorBody(validationMsg("saippua")))
    }
  }

}
