package fi.oph.kouta.integration

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData.Osoite1
import fi.oph.kouta.TestOids.{ChildOid, EvilCousin, OphOid, UnknownOid}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain._
import fi.oph.kouta.repository.OppilaitosDAO
import fi.oph.kouta.security.{Authority, CasSession, ServiceTicket}
import fi.oph.kouta.service.{KoodistoService, KoutaValidationException, OppilaitoksenOsaServiceValidation}
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.validation.ExternalQueryResults.itemFound
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{ErrorMessage, ValidationError}
import org.mockito.scalatest.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.contain
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.{Assertion, BeforeAndAfterEach}

import java.net.InetAddress
import java.util.UUID
import scala.util.{Failure, Try}

class OppilaitoksenOsaServiceValidationSpec extends AnyFlatSpec with BeforeAndAfterEach with MockitoSugar {
  val koodistoService     = mock[KoodistoService]
  val oppilaitosDao       = mock[OppilaitosDAO]

  val min = TestData.MinOppilaitoksenOsa
  val max = TestData.JulkaistuOppilaitoksenOsa
  val maxMetadata = max.metadata.get
  val invalidOsoite = Some(Osoite1.copy(postinumeroKoodiUri = Some("puppu")))
  def minWithYhteystieto(yt: Yhteystieto): OppilaitoksenOsa =
    min.copy(metadata = Some(OppilaitoksenOsaMetadata(hakijapalveluidenYhteystiedot = Some(yt))))

  val authenticatedPaakayttaja = Authenticated(
    UUID.randomUUID().toString,
    CasSession(
      ServiceTicket("ST-123"),
      "1.2.3.1234",
      Set("APP_KOUTA", "APP_KOUTA_OPHPAAKAYTTAJA", s"APP_KOUTA_OPHPAAKAYTTAJA_${OphOid}").map(Authority(_))
    ),
    "testAgent",
    InetAddress.getByName("127.0.0.1")
  )
  val authenticatedNonPaakayttaja = Authenticated(
    UUID.randomUUID().toString,
    CasSession(
      ServiceTicket("ST-123"),
      "1.2.3.1234",
      Set("APP_KOUTA", "APP_KOUTA_HAKUKOHDE_READ", s"APP_KOUTA_HAKUKOHDE_READ_${ChildOid}").map(Authority(_))
    ),
    "testAgent",
    InetAddress.getByName("127.0.0.1")
  )

  val validator = new OppilaitoksenOsaServiceValidation(koodistoService, oppilaitosDao)

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(koodistoService.koodiUriExistsInKoodisto(PostiosoiteKoodisto, "posti_04230#2")).thenAnswer(itemFound)
    when(koodistoService.koodiUriExistsInKoodisto(PostiosoiteKoodisto, "posti_61100#2")).thenAnswer(itemFound)
    when(oppilaitosDao.getTila(ChildOid)).thenAnswer(Some(Julkaistu))
    when(oppilaitosDao.getTila(UnknownOid)).thenAnswer(None)
    when(oppilaitosDao.getTila(EvilCousin)).thenAnswer(Some(Tallennettu))
  }

  def passesValidation(
    oppilaitoksenOsa: OppilaitoksenOsa,
    oldOppilaitoksenOsa: Option[OppilaitoksenOsa] = None,
    authenticated: Authenticated = authenticatedNonPaakayttaja
  ): Unit = validator.withValidation(oppilaitoksenOsa, oldOppilaitoksenOsa, authenticated)(o => o)

  def failsValidation(
   oppilaitoksenOsa: OppilaitoksenOsa,
   path: String,
   message: ErrorMessage
  ): Assertion = failsValidation(oppilaitoksenOsa, Seq(ValidationError(path, message)), authenticatedNonPaakayttaja)

  def failsValidation(
     oppilaitoksenOsa: OppilaitoksenOsa,
     expected: Seq[ValidationError],
     authenticated: Authenticated = authenticatedNonPaakayttaja
   ): Assertion =
    Try(validator.withValidation(oppilaitoksenOsa, None, authenticated)(o => o)) match {
      case Failure(exp: KoutaValidationException) => exp.errorMessages should contain theSameElementsAs expected
      case _ => fail("Expecting validation failure, but it succeeded")
    }

  "Oppilaitoksen osa validation" should "succeed when new valid oppilaitoksen osa" in {
    passesValidation(max)
  }

  it should "succeed when incomplete luonnos" in {
    passesValidation(min)
  }

  it should "fail if perustiedot are invalid" in {
    failsValidation(max.copy(oid = OrganisaatioOid("virhe")), "oid", validationMsg("virhe"))
    failsValidation(min.copy(kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(max.copy(organisaatioOid = OrganisaatioOid("virhe")), "organisaatioOid", validationMsg("virhe"))
    failsValidation(min.copy(teemakuva = Some("url")), "teemakuva", invalidUrl("url"))
    failsValidation(
      max.copy(teemakuva = Some("https://example.com/test.jpg")),
      "teemakuva",
      invalidUrlDomain(
        "https://example.com/test.jpg",
        Set("https://konfo-files.opintopolku.fi", "https://konfo-files.untuvaopintopolku.fi")
      )
    )
  }

  it should "fail if oppilaitosOid invalid" in {
    failsValidation(max.copy(oppilaitosOid = OrganisaatioOid("virhe")), "oppilaitosOid", validationMsg("virhe"))
    failsValidation(max.copy(oppilaitosOid = UnknownOid), "oppilaitosOid", nonExistent("Oppilaitosta", UnknownOid))
    failsValidation(max.copy(oppilaitosOid = EvilCousin), "tila", notYetJulkaistu("Oppilaitosta", EvilCousin))
  }

  "Metadata validation" should "fail if invalid wwwSivu" in {
    failsValidation(
      min.copy(metadata =
        Some(OppilaitoksenOsaMetadata(wwwSivu = Some(NimettyLinkki(url = Map(Fi -> "http://testi.fi", Sv -> "puppu")))))
      ),
      "metadata.wwwSivu.url.sv",
      invalidUrl("puppu")
    )
  }

  it should "fail if invalid esittelyvideo" in {
    failsValidation(
      min.copy(metadata =
        Some(OppilaitoksenOsaMetadata(esittelyvideo = Some(NimettyLinkki(url = Map(Fi -> "http://testi.fi", Sv -> "puppu")))))
      ),
      "metadata.esittelyvideo.url.sv",
      invalidUrl("puppu")
    )
    failsValidation(
      max.copy(metadata = Some(maxMetadata.copy(esittelyvideo = Some(NimettyLinkki(url = Map(), nimi = vainSuomeksi))))),
      Seq(
        ValidationError("metadata.esittelyvideo.url", invalidKielistetty(Seq(Fi, Sv))),
        ValidationError("metadata.esittelyvideo.nimi", invalidKielistetty(Seq(Sv)))
      )
    )
  }

  it should "fail if both teemakuva and esittelyvideo are defined" in {
    failsValidation(
      min.copy(
        teemakuva = Some("https://konfo-files.untuvaopintopolku.fi/test.jpg"),
        metadata = Some(OppilaitoksenOsaMetadata(esittelyvideo = Some(NimettyLinkki(url = Map(Fi -> "http://testi.fi/video")))))),
      "teemakuva",
      onlyTeemakuvaOrEsittelyvideoAllowed
    )
  }

  it should "fail if opiskelijoita -amount is negative" in {
    failsValidation(
      min.copy(metadata =
        Some(OppilaitoksenOsaMetadata(opiskelijoita = Some(-1)))
      ),
      "metadata.opiskelijoita",
      notNegativeMsg
    )
  }

  it should "fail if missing values in julkaistu oppilaitoksen osa" in {
    failsValidation(max.copy(metadata = Some(maxMetadata.copy(kampus = vainSuomeksi, esittely = vainSuomeksi, wwwSivu = None))), Seq(
      ValidationError("metadata.kampus", kielistettyWoSvenskaError),
      ValidationError("metadata.esittely", kielistettyWoSvenskaError),
      ValidationError("metadata.wwwSivu", missingMsg)
    ))
  }

  "Yhteystieto validation" should "succeed when postiosoite not changed, eventhough unknown postinumeroKoodiUri" in {
    val osa = minWithYhteystieto(Yhteystieto(postiosoite = invalidOsoite))
    passesValidation(osa, Some(osa))
  }

  it should "fail if invalid data" in {
    failsValidation(minWithYhteystieto(Yhteystieto(kayntiosoite = invalidOsoite)), "metadata.hakijapalveluidenYhteystiedot.kayntiosoite.postinumeroKoodiUri", invalidPostiosoiteKoodiUri("puppu"))
  }

  it should "pass if oph pääkäyttäjä changes järjestää uheilijan ammatillista koulutus" in {
    passesValidation(max.copy(metadata = Some(maxMetadata.copy(jarjestaaUrheilijanAmmKoulutusta = Some(true)))), Some(max), authenticatedPaakayttaja)
  }

  it should "fail if non oph pääkäyttäjä changes järjestää uheilijan ammatillista koulutus" in {
    failsValidation(
      max.copy(metadata = Some(maxMetadata.copy(jarjestaaUrheilijanAmmKoulutusta = Some(true)))),
      "metadata.jarjestaaUrheilijanAmmKoulutusta",
      ErrorMessage(
        msg = "Vain OPH:n pääkäyttäjä voi muuttaa tiedon järjestääkö oppilaitoksen osa urheilijan ammatillista koulutusta",
        id = "invalidRightsForChangingJarjestaaUrheilijanAmmatillistaKoulutusta"
      )
    )
  }

  val vainSuomeksi  = Map(Fi -> "vain suomeksi", Sv -> "")
  val kielistettyWoSvenskaError = invalidKielistetty(Seq(Sv))
}
