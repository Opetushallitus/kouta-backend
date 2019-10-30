package fi.oph.kouta.integration

import java.time.{Duration, Instant, ZoneId}

import fi.oph.kouta.TestData
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.fixture.{KoulutusFixture, ToteutusFixture}
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.util.TimeUtils
import fi.oph.kouta.validation.Validations
import org.json4s.jackson.Serialization.read

import scala.util.Success

class KoulutusSpec extends KoutaIntegrationSpec with AccessControlSpec with KoulutusFixture with ToteutusFixture with Validations {

  override val roleEntities = Seq(Role.Koulutus)

  "Get koulutus by oid" should "return 404 if koulutus not found" in {
    get(s"$KoulutusPath/123", headers = defaultHeaders) {
      status should equal (404)
      body should include ("Unknown koulutus oid")
    }
  }

  it should "return 401 without a session" in {
    get(s"$KoulutusPath/123", headers = Map.empty) {
      status should equal (401)
    }
  }

  it should "allow the user of the koulutus organization to read the koulutus" in {
    val oid = put(koulutus)
    get(oid, crudSessions(ChildOid), koulutus(oid))
  }

  it should "deny an authenticated user without organization access to access unpublished koulutus" in {
    val oid = put(koulutus)
    get(s"$KoulutusPath/$oid", crudSessions(LonelyOid), 403)
  }

  it should "deny the user of a tarjoaja organization without access to the koulutus organization to read the koulutus" in {
    val oid = put(koulutus.copy(tarjoajat = List(LonelyOid)))
    get(s"$KoulutusPath/$oid", crudSessions(LonelyOid), 403)
  }

  it should "allow the user of a parent organization to read the koulutus" in {
    val oid = put(koulutus)
    get(oid, crudSessions(ParentOid), koulutus(oid))
  }

  it should "deny the user of a child organization to read the koulutus" in {
    val oid = put(koulutus)
    get(s"$KoulutusPath/$oid", crudSessions(GrandChildOid), 403)
  }

  it should "deny the user of a wrong role to read the koulutus" in {
    val oid = put(koulutus)
    get(s"$KoulutusPath/$oid", otherRoleSession, 403)
  }

  it should "allow the indexer to read any koulutus" in {
    val oid = put(koulutus)
    get(s"$KoulutusPath/$oid", indexerSession, 200)
  }

  it should "allow a user of similar oppilaitostyyppi to access public koulutus" in {
    val oid = put(koulutus.copy(julkinen = true), crudSessions(koulutus.organisaatioOid))
    get(oid, crudSessions(EvilChildOid), koulutus(oid).copy(julkinen = true))
  }

  it should "deny an authenticated user of a different oppilaitostyyppi to access public koulutus" in {
    val oid = put(koulutus.copy(julkinen = true))
    get(s"$KoulutusPath/$oid", readSessions(YoOid), 403)
  }

  "Create koulutus" should "store koulutus" in {
    val oid = put(koulutus)
    get(oid, koulutus(oid))
  }

  it should "store korkeakoulutus koulutus" in {
    val oid = put(TestData.YoKoulutus)
    get(oid, TestData.YoKoulutus.copy(oid = Some(KoulutusOid(oid))))
  }

  it should "validate new koulutus" in {
    put(KoulutusPath, bytes(koulutus.copy(koulutusKoodiUri = None)), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validateErrorBody(missingMsg("koulutusKoodiUri")))
    }
  }

  it should "return 401 without a session" in {
    put(KoulutusPath, bytes(koulutus), Seq(jsonHeader)) {
      withClue(body) {
        status should equal(401)
      }
    }
  }

  it should "allow access if the user has rights to the koulutus organization" in {
    put(koulutus.copy(tarjoajat = List.empty), crudSessions(ChildOid))
  }

  it should "deny access if the user is missing rights to the koulutus organization" in {
    put(KoulutusPath, koulutus.copy(tarjoajat = List.empty), crudSessions(EvilChildOid), 403)
  }

  it should "allow access if the user has rights to an ancestor of the koulutus organization" in {
    put(koulutus, crudSessions(ParentOid))
  }

  it should "deny access if the user only has rights to a descendant of the koulutus organization" in {
    put(KoulutusPath, koulutus, crudSessions(EvilChildOid), 403)
  }

  it should "fail if the user doesn't have the right role" in {
    put(KoulutusPath, koulutus, otherRoleSession, 403)
  }

  it should "deny access for the indexer" in {
    put(KoulutusPath, koulutus, indexerSession, 403)
  }

  it should "allow access even if the user is missing rights to some of the tarjoajat" in {
    put(koulutus, crudSessions(ChildOid))
  }

  "Update koulutus" should "update koulutus" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    update(koulutus(oid, Arkistoitu), lastModified)
    get(oid, koulutus(oid, Arkistoitu))
  }

  it should "not update koulutus" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    update(koulutus(oid), lastModified, false)
    get(oid, koulutus(oid))
  }

  it should "return 401 without a session" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    post(KoulutusPath, bytes(koulutus(oid)), Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified)) {
      status should equal (401)
    }
  }

  it should "allow access if the user has rights to the koulutus organization" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    update(koulutus(oid), lastModified, expectUpdate = false, crudSessions(ChildOid))
  }

  it should "deny access if the user is missing rights to the koulutus organization" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    update(koulutus(oid), lastModified, crudSessions(EvilChildOid), 403)
  }

  it should "allow access if the user has rights to an ancestor organization" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    update(koulutus(oid), lastModified, expectUpdate = false, crudSessions(ParentOid))
  }

  it should "deny access if the user only has rights to a descent organization" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    update(koulutus(oid), lastModified, crudSessions(GrandChildOid), 403)
  }

  it should "deny access if the user doesn't have update rights" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    update(koulutus(oid), lastModified, readSessions(ChildOid), 403)
  }

  it should "deny access for the indexer" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    update(koulutus(oid), lastModified, indexerSession, 403)
  }

  it should "allow access even if the user doesn't have rights to a tarjoaja organization being removed" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    val updatedKoulutus = koulutus(oid).copy(tarjoajat = koulutus.tarjoajat diff Seq(EvilCousin))

    update(updatedKoulutus, lastModified, expectUpdate = true, crudSessions(ChildOid))
  }

  it should "allow access even if the user doesn't have rights to a tarjoaja organization being added" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    val updatedKoulutus = koulutus(oid).copy(tarjoajat = EvilChildOid :: koulutus.tarjoajat)
    update(updatedKoulutus, lastModified, expectUpdate = true, crudSessions(ChildOid))
  }

  it should "fail update if 'x-If-Unmodified-Since' header is missing" in {
    val oid = put(koulutus)
    post(KoulutusPath, bytes(koulutus(oid)), defaultHeaders) {
      status should equal (400)
      body should equal (errorBody(s"Otsake ${KoutaServlet.IfUnmodifiedSinceHeader} on pakollinen."))
    }
  }

  it should "fail update if modified in between get and update" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    Thread.sleep(1500)
    update(koulutus(oid, Arkistoitu), lastModified)
    post(KoulutusPath, bytes(koulutus(oid)), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "update koulutuksen tekstit ja tarjoajat" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    val uusiKoulutus = koulutus(oid).copy(
      kielivalinta = Seq(Fi, Sv, En),
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      tarjoajat = List("2.2", "3.2", "4.2").map(OrganisaatioOid))
    update(uusiKoulutus, lastModified, true)
    get(oid, uusiKoulutus)
  }

  it should "update the modified time of the koulutus even when just tarjoajat is updated" in {
    val oid = put(koulutus)

    setModifiedToPast(oid, "10 minutes") should be(Success(()))
    val lastModified = get(oid, koulutus(oid))
    val lastModifiedInstant = TimeUtils.parseHttpDate(lastModified)
    Duration.between(lastModifiedInstant, Instant.now).compareTo(Duration.ofMinutes(5)) should equal(1)

    val uusiKoulutus = koulutus(oid).copy(tarjoajat = List("2.2", "3.2", "4.2").map(OrganisaatioOid))
    update(uusiKoulutus, lastModified, expectUpdate = true)

    get(s"$KoulutusPath/$oid", headers = defaultHeaders) {
      status should equal(200)

      val koulutus = read[Koulutus](body)
      koulutus.modified.isDefined should be(true)
      val modifiedInstant = koulutus.modified.get.atZone(ZoneId.of("Europe/Helsinki")).toInstant

      Duration.between(lastModifiedInstant, modifiedInstant).compareTo(Duration.ofMinutes(5)) should equal(1)
      Duration.between(lastModifiedInstant, modifiedInstant).compareTo(Duration.ofMinutes(15)) should equal(-1)
    }
  }

  it should "delete all tarjoajat and read last modified from history" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    Thread.sleep(1500)
    val uusiKoulutus = koulutus(oid).copy(tarjoajat = List())
    update(uusiKoulutus, lastModified, true)
    get(oid, uusiKoulutus) should not equal (lastModified)
  }

  it should "store and update unfinished koulutus" in {
    val unfinishedKoulutus = new Koulutus(johtaaTutkintoon = true, muokkaaja = UserOid("5.4.3.2.1"), organisaatioOid = OrganisaatioOid("1.2"), modified = None)
    val oid = put(unfinishedKoulutus)
    val lastModified = get(oid, unfinishedKoulutus.copy(oid = Some(KoulutusOid(oid))))
    val newUnfinishedKoulutus = unfinishedKoulutus.copy(oid = Some(KoulutusOid(oid)), johtaaTutkintoon = false)
    update(newUnfinishedKoulutus, lastModified)
    get(oid, newUnfinishedKoulutus)
  }

  it should "validate updated koulutus" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    post(KoulutusPath, bytes(koulutus(oid).copy(koulutusKoodiUri = None)), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validateErrorBody(missingMsg("koulutusKoodiUri")))
    }
  }

  "List toteutukset related to koulutus" should "return all toteutukset related to koulutus" in {
    val oid = put(koulutus)
    val t1 = put(toteutus(oid))
    val t2 = put(toteutus(oid))
    val t3 = put(toteutus(oid))
    get(s"$KoulutusPath/$oid/toteutukset", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[Toteutus]](body) should contain theSameElementsAs(List(
        toteutus(t1, oid).copy(modified = Some(readModifiedByOid(t1, "toteutukset"))),
        toteutus(t2, oid).copy(modified = Some(readModifiedByOid(t2, "toteutukset"))),
        toteutus(t3, oid).copy(modified = Some(readModifiedByOid(t3, "toteutukset")))
      ))
    }
  }

  it should "return empty result if koulutus has no toteutukset" in {
    val oid = put(koulutus)
    get(s"$KoulutusPath/$oid/toteutukset", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[Toteutus]](body) should contain theSameElementsAs(List())
    }
  }

  it should "deny access without a valid session" in {
    val oid = KoulutusOid("oid")
    get(s"$KoulutusPath/$oid/toteutukset", headers = Seq()) {
      withClue(body) {
        status should equal (401)
      }
    }
  }

  it should "deny access without the indexer role" in {
    val oid = KoulutusOid("oid")
    get(s"$KoulutusPath/$oid/toteutukset", defaultSessionId, 403)
  }

  it should "deny access without root organization access to the indexer role" in {
    val oid = KoulutusOid("oid")
    get(s"$KoulutusPath/$oid/toteutukset", fakeIndexerSession, 403)
  }

  "List koulutukset by tarjoaja" should "list all koulutukset distinct" in {
    val oid = put(koulutus.copy(tarjoajat = List(ChildOid, GrandChildOid)))
    get(s"$KoulutusPath/tarjoaja/$ChildOid", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[Koulutus]](body).filter(_.oid.get.s == oid).size should equal (1)
    }




  }
}
