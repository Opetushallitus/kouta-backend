package fi.oph.kouta.integration

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData.Osoite1
import fi.oph.kouta.TestOids.{ChildOid, OphOid}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain._
import fi.oph.kouta.security.{Authority, CasSession, ServiceTicket}
import fi.oph.kouta.service.{KoodistoService, KoutaValidationException, OppilaitosServiceValidation}
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

class OppilaitosServiceValidationSpec extends AnyFlatSpec with BeforeAndAfterEach with MockitoSugar {
  val koodistoService     = mock[KoodistoService]

  val min         = TestData.MinOppilaitos
  val max         = TestData.JulkaistuOppilaitos
  val maxMetadata = max.metadata.get

  val invalidOsoite = Some(Osoite1.copy(postinumeroKoodiUri = Some("puppu")))
  def minWithYhteystieto(yt: Yhteystieto): Oppilaitos =
    min.copy(metadata = Some(OppilaitosMetadata(hakijapalveluidenYhteystiedot = Some(yt))))

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

  val validator = new OppilaitosServiceValidation(koodistoService)
  override def beforeEach(): Unit = {
    super.beforeEach()
    when(koodistoService.koodiUriExistsInKoodisto(TietoaOpiskelustaKoodisto, "organisaationkuvaustiedot_03#1")).thenAnswer(itemFound)
    when(koodistoService.koodiUriExistsInKoodisto(PostiosoiteKoodisto, "posti_04230#2")).thenAnswer(itemFound)
    when(koodistoService.koodiUriExistsInKoodisto(PostiosoiteKoodisto, "posti_61100#2")).thenAnswer(itemFound)
  }

  def passesValidation(
    oppilaitos: Oppilaitos,
    oldOppilaitos: Option[Oppilaitos] = None,
    authenticated: Authenticated = authenticatedNonPaakayttaja
  ): Unit = validator.withValidation(oppilaitos, oldOppilaitos, authenticated)(o => o)

  def failsValidation(
     oppilaitos: Oppilaitos,
     path: String,
     message: ErrorMessage
  ): Assertion = failsValidation(oppilaitos, Seq(ValidationError(path, message)), authenticatedNonPaakayttaja)

  def failsValidation(
                       oppilaitos: Oppilaitos,
                       expected: Seq[ValidationError],
                       authenticated: Authenticated = authenticatedNonPaakayttaja
                     ): Assertion =
    Try(validator.withValidation(oppilaitos, None, authenticated)(o => o)) match {
      case Failure(exp: KoutaValidationException) => exp.errorMessages should contain theSameElementsAs expected
      case _ => fail("Expecting validation failure, but it succeeded")
    }

  "Oppilaitos validation" should "pass a valid oppilaitos" in {
    passesValidation(max)
  }

  it should "pass an incomplete luonnos oppilaitos" in {
    passesValidation(min)
  }

  it should "fail if perustiedot are invalid" in {
    failsValidation(max.copy(oid = OrganisaatioOid("virhe")), "oid", validationMsg("virhe"))
    failsValidation(min.copy(kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(max.copy(organisaatioOid = OrganisaatioOid("virhe")), "organisaatioOid", validationMsg("virhe"))
    failsValidation(min.copy(teemakuva = Some("url")), "teemakuva", invalidUrl("url"))
    failsValidation(min.copy(logo = Some("ftp://url.fi/ftp-logo")), "logo", invalidUrl("ftp://url.fi/ftp-logo"))
  }

  "Metadata validation" should "succeed when tietoa opiskelusta -data not changed, eventhough unknown otsikkoKoodiUri" in {
    val oppilaitos =
      min.copy(metadata = Some(OppilaitosMetadata(tietoaOpiskelusta = Seq(TietoaOpiskelusta("puppu", Map())))))
    passesValidation(oppilaitos, Some(oppilaitos))
  }

  it should "fail if invalid tietoa opiskelusta -data" in {
    failsValidation(
      min.copy(metadata = Some(OppilaitosMetadata(tietoaOpiskelusta = Seq(TietoaOpiskelusta("puppu", Map()))))),
      "metadata.tietoaOpiskelusta[0].otsikkoKoodiUri",
      invalidTietoaOpiskelustaOtsikkoKoodiUri("puppu")
    )
    failsValidation(
      max.copy(metadata =
        Some(maxMetadata.copy(tietoaOpiskelusta = Seq(TietoaOpiskelusta("organisaationkuvaustiedot_03#1", Map()))))
      ),
      "metadata.tietoaOpiskelusta[0].teksti",
      invalidKielistetty(Seq(Fi, Sv))
    )
  }

  it should "fail if invalid wwwSivu" in {
    failsValidation(
      min.copy(metadata =
        Some(OppilaitosMetadata(wwwSivu = Some(NimettyLinkki(url = Map(Fi -> "http://testi.fi", Sv -> "puppu")))))
      ),
      "metadata.wwwSivu.url.sv",
      invalidUrl("puppu")
    )
    failsValidation(
      max.copy(metadata = Some(maxMetadata.copy(wwwSivu = Some(NimettyLinkki(url = Map(), nimi = vainSuomeksi))))),
      Seq(
        ValidationError("metadata.wwwSivu.url", invalidKielistetty(Seq(Fi, Sv))),
        ValidationError("metadata.wwwSivu.nimi", invalidKielistetty(Seq(Sv)))
      )
    )
  }

  it should "fail if any of the numeric fields negative" in {
    failsValidation(
      min.copy(metadata =
        Some(
          OppilaitosMetadata(
            opiskelijoita = Some(-1),
            korkeakouluja = Some(-1),
            tiedekuntia = Some(-1),
            kampuksia = Some(-1),
            yksikoita = Some(-1),
            toimipisteita = Some(-1),
            akatemioita = Some(-1)
          )
        )
      ),
      Seq(
        ValidationError("metadata.opiskelijoita", notNegativeMsg),
        ValidationError("metadata.korkeakouluja", notNegativeMsg),
        ValidationError("metadata.tiedekuntia", notNegativeMsg),
        ValidationError("metadata.kampuksia", notNegativeMsg),
        ValidationError("metadata.yksikoita", notNegativeMsg),
        ValidationError("metadata.toimipisteita", notNegativeMsg),
        ValidationError("metadata.akatemioita", notNegativeMsg)
      )
    )
  }

  it should "fail if missing values in julkaistu oppilaitos" in {
    failsValidation(
      max.copy(metadata = Some(maxMetadata.copy(esittely = vainSuomeksi, wwwSivu = None))),
      Seq(
        ValidationError("metadata.esittely", invalidKielistetty(Seq(Sv))),
        ValidationError("metadata.wwwSivu", missingMsg)
      )
    )
  }

  "Yhteystieto validation" should "succeed when postiosoite not changed, eventhough unknown postinumeroKoodiUri" in {
    val oppilaitos = minWithYhteystieto(Yhteystieto(postiosoite = invalidOsoite))
    passesValidation(oppilaitos, Some(oppilaitos))
  }

  it should "succeed when kayntiosoite not changed, eventhough unknown postinumeroKoodiUri" in {
    val oppilaitos = minWithYhteystieto(Yhteystieto(kayntiosoite = invalidOsoite))
    passesValidation(oppilaitos, Some(oppilaitos))
  }

  it should "fail if invalid postiosoite" in {
    failsValidation(
      minWithYhteystieto(Yhteystieto(postiosoite = invalidOsoite)),
      "metadata.hakijapalveluidenYhteystiedot.postiosoite.postinumeroKoodiUri",
      invalidPostiosoiteKoodiUri("puppu")
    )
  }

  it should "fail if invalid kayntiosoite" in {
    failsValidation(
      minWithYhteystieto(Yhteystieto(kayntiosoite = invalidOsoite)),
      "metadata.hakijapalveluidenYhteystiedot.kayntiosoite.postinumeroKoodiUri",
      invalidPostiosoiteKoodiUri("puppu")
    )
  }

  it should "fail if invalid email" in {
    failsValidation(
      minWithYhteystieto(Yhteystieto(sahkoposti = Map(Fi -> "puppu"))),
      "metadata.hakijapalveluidenYhteystiedot.sahkoposti.fi",
      invalidEmail("puppu")
    )
  }

  it should "fail if missing values in julkaistu oppilaitos" in {
    val yt = maxMetadata.hakijapalveluidenYhteystiedot.get
    failsValidation(
      max.copy(metadata =
        Some(
          maxMetadata.copy(hakijapalveluidenYhteystiedot =
            Some(yt.copy(nimi = Map(), puhelinnumero = vainSuomeksi, sahkoposti = Map(Fi -> "opettaja@koulu.fi")))
          )
        )
      ),
      Seq(
        ValidationError("metadata.hakijapalveluidenYhteystiedot.nimi", invalidKielistetty(Seq(Fi, Sv))),
        ValidationError("metadata.hakijapalveluidenYhteystiedot.puhelinnumero", invalidKielistetty(Seq(Sv))),
        ValidationError("metadata.hakijapalveluidenYhteystiedot.sahkoposti", invalidKielistetty(Seq(Sv)))
      )
    )
  }

  it should "pass if oph pääkäyttäjä changes järjestää uheilijan ammatillista koulutusta" in {
    passesValidation(max.copy(metadata = Some(maxMetadata.copy(jarjestaaUrheilijanAmmKoulutusta = Some(true)))), Some(max), authenticatedPaakayttaja)
  }

  it should "fail if non oph pääkäyttäjä changes järjestää uheilijan ammatillista koulutusta" in {
    failsValidation(
      max.copy(metadata = Some(maxMetadata.copy(jarjestaaUrheilijanAmmKoulutusta = Some(true)))),
      "metadata.jarjestaaUrheilijanAmmKoulutusta",
      ErrorMessage(
        msg = "Vain OPH:n pääkäyttäjä voi muuttaa tiedon järjestääkö oppilaitos urheilijan ammatillista koulutusta",
        id = "invalidRightsForChangingJarjestaaUrheilijanAmmatillistaKoulutusta"
      )
    )
  }

  it should "not fail if non oph pääkäyttäjä has not changes järjestää uheilijan ammatillista koulutusta" in {
    passesValidation(
      max.copy(metadata = Some(maxMetadata.copy(jarjestaaUrheilijanAmmKoulutusta = Some(false)))),
      Some(max)
    )
  }


  val vainSuomeksi  = Map(Fi -> "vain suomeksi", Sv -> "")
}
