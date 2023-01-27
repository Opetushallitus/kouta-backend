package fi.oph.kouta.integration

import fi.oph.kouta.TestData.{inFuture, inPast}
import fi.oph.kouta.client.{HakemusPalveluClient, CachedKoodistoClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.validation.CrudOperations.{CrudOperation, create, update}
import fi.oph.kouta.validation.ExternalQueryResults.{itemFound, itemNotFound, queryFailed}
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation._
import org.mockito.scalatest.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.{contain, equal}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.{Assertion, BeforeAndAfterEach}

import java.util.UUID

class CommonServiceValidationSpec extends AnyFlatSpec with BeforeAndAfterEach with MockitoSugar {
  val koodistoClient  = mock[CachedKoodistoClient]
  val hakemusPalveluClient = mock[HakemusPalveluClient]

  val vainSuomeksi         = Map(Fi -> "vain suomeksi", Sv -> "")
  val kielistettyWoSvenska = invalidKielistetty(Seq(Sv))
  val fullKielistetty      = Map(Fi -> "suomeksi", Sv -> "på svenska")
  val kielet               = Seq(Fi, Sv)
  val ataruId              = UUID.randomUUID()

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(koodistoClient.koodiUriExistsInKoodisto(KoulutuksenLisatiedotKoodisto, "koulutuksenlisatiedot_03#1")).thenAnswer(itemFound)
    when(koodistoClient.koodiUriExistsInKoodisto(PostiosoiteKoodisto, "posti_04230#2")).thenAnswer(itemFound)
  }

  def failsValidation(
      e: KoulutuksenAlkamiskausi,
      tila: Julkaisutila,
      expected: Seq[ValidationError]
  ): Assertion =
    e.validate("path", Some(e), ValidationContext(tila, kielet, create), koodistoClient.koodiUriExistsInKoodisto(KausiKoodisto, _)) match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors   => errors should contain theSameElementsAs expected
    }

  def failsValidationOnJulkaisu(
      e: KoulutuksenAlkamiskausi,
      expected: Seq[ValidationError]
  ): Assertion =
    e.validateOnJulkaisu("path") match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors   => errors should contain theSameElementsAs expected
    }

  "KoulutuksenAlkamiskausi validation" should "Succeed if alkamiskausiKoodiUri not changed in modify operation" in {
    KoulutuksenAlkamiskausi(
      alkamiskausityyppi = Some(TarkkaAlkamisajankohta),
      koulutuksenAlkamiskausiKoodiUri = Some("kausi_xx#2")
    ).validate(
      "path",
      None,
      ValidationContext(Tallennettu, kielet, update),
      koodistoClient.koodiUriExistsInKoodisto(KausiKoodisto, _)
    ) match {
      case NoErrors =>
      case errors   => fail("Expected no errors, but received: " + errors)
    }
  }

  it should "fail if invalid AlkamiskausiKoodiUri" in {
    failsValidation(
      KoulutuksenAlkamiskausi(
        alkamiskausityyppi = Some(TarkkaAlkamisajankohta),
        koulutuksenAlkamiskausiKoodiUri = Some("kausi_xx#2")
      ),
      Tallennettu,
      Seq(ValidationError("path.koulutuksenAlkamiskausiKoodiUri", invalidKausiKoodiuri("kausi_xx#2")))
    )
  }

  it should "fail if invalid time values" in {
    failsValidation(
      KoulutuksenAlkamiskausi(
        alkamiskausityyppi = Some(TarkkaAlkamisajankohta),
        koulutuksenAlkamispaivamaara = Some(inFuture(2000)),
        koulutuksenPaattymispaivamaara = Some(inFuture(1000)),
        koulutuksenAlkamisvuosi = Some("10000")
      ),
      Tallennettu,
      Seq(
        ValidationError("path.koulutuksenAlkamispaivamaara", InvalidKoulutuspaivamaarat),
        ValidationError("path.koulutuksenAlkamisvuosi", validationMsg("10000"))
      )
    )
  }

  it should "fail if values missing from julkaistu KoulutuksenAlkamiskausi" in {
    failsValidation(
      KoulutuksenAlkamiskausi(
        alkamiskausityyppi = None,
        henkilokohtaisenSuunnitelmanLisatiedot = vainSuomeksi
      ),
      Julkaistu,
      Seq(
        ValidationError("path.alkamiskausityyppi", missingMsg),
        ValidationError("path.henkilokohtaisenSuunnitelmanLisatiedot", kielistettyWoSvenska)
      )
    )
    failsValidation(
      KoulutuksenAlkamiskausi(
        alkamiskausityyppi = Some(TarkkaAlkamisajankohta)
      ),
      Julkaistu,
      Seq(
        ValidationError("path.koulutuksenAlkamispaivamaara", missingMsg)
      )
    )
    failsValidation(
      KoulutuksenAlkamiskausi(
        alkamiskausityyppi = Some(AlkamiskausiJaVuosi)
      ),
      Julkaistu,
      Seq(
        ValidationError("path.koulutuksenAlkamiskausiKoodiUri", missingMsg),
        ValidationError("path.koulutuksenAlkamisvuosi", missingMsg)
      )
    )
  }

  it should "fail if alkamiskausi not in future in julkaistu KoulutuksenAlkamiskausi" in {
    val inPastJakso = KoulutuksenAlkamiskausi(
      alkamiskausityyppi = Some(TarkkaAlkamisajankohta),
      koulutuksenAlkamispaivamaara = Some(inPast(1000)),
      koulutuksenPaattymispaivamaara = Some(inPast(500)),
      koulutuksenAlkamisvuosi = Some("2020")
    )
    failsValidationOnJulkaisu(
      inPastJakso,
      Seq(
        ValidationError(
          "path.koulutuksenAlkamisvuosi",
          pastDateMsg(inPastJakso.koulutuksenAlkamisvuosi.get)
        ),
        ValidationError(
          "path.koulutuksenAlkamispaivamaara",
          pastDateMsg(inPastJakso.koulutuksenAlkamispaivamaara.get)
        ),
        ValidationError(
          "path.koulutuksenPaattymispaivamaara",
          pastDateMsg(inPastJakso.koulutuksenPaattymispaivamaara.get)
        )
      )
    )
  }

  def failsValidation(e: Lisatieto, tila: Julkaisutila, expected: Seq[ValidationError]): Assertion =
    e.validate(
      "path",
      Some(e),
      ValidationContext(tila, kielet, create),
      koodistoClient.koodiUriExistsInKoodisto(KoulutuksenLisatiedotKoodisto, _)
    ) match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors   => errors should contain theSameElementsAs expected
    }

  "Lisatieto validation" should "succeed if lisatiedotOtsikkoKoodiUri not changed in modify operation" in {
    Lisatieto("koulutuksenlisatiedot_99#1", fullKielistetty).validate(
      "path",
      None,
      ValidationContext(Julkaistu, kielet, update),
      koodistoClient.koodiUriExistsInKoodisto(KoulutuksenLisatiedotKoodisto, _)
    ) match {
      case NoErrors =>
      case errors   => fail("Expected no errors, but received: " + errors)
    }
  }

  it should "fail if invalid lisatiedotOtsikkoKoodiUri" in {
    failsValidation(
      Lisatieto(otsikkoKoodiUri = "koulutuksenlisatiedot_99#1", Map()),
      Tallennettu,
      Seq(ValidationError("path.otsikkoKoodiUri", invalidLisatietoOtsikkoKoodiuri("koulutuksenlisatiedot_99#1")))
    )
  }

  it should "fail if values missing from julkaistu Lisatieto" in {
    failsValidation(
      Lisatieto(otsikkoKoodiUri = "koulutuksenlisatiedot_03#1", Map()),
      Julkaistu,
      Seq(ValidationError("path.teksti", invalidKielistetty(Seq(Fi, Sv))))
    )
  }

  def failsValidation(
      e: Osoite,
      tila: Julkaisutila,
      expected: Seq[ValidationError]
  ): Assertion =
    e.validate(
      "path",
      Some(e),
      ValidationContext(tila, kielet, create),
      koodistoClient.koodiUriExistsInKoodisto(PostiosoiteKoodisto, _)
    ) match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors   => errors should contain theSameElementsAs expected
    }

  "Osoite validation" should "Succeed if postinumeroKoodiUri not changed in modify operation" in {
    Osoite(postinumeroKoodiUri = Some("posti_99999#2")).validate(
      "path",
      None,
      ValidationContext(Tallennettu, kielet, update),
      koodistoClient.koodiUriExistsInKoodisto(PostiosoiteKoodisto, _)
    ) match {
      case NoErrors =>
      case errors   => fail("Expected no errors, but received: " + errors)
    }
  }

  it should "fail if unknown postinumeroKoodiUri" in {
    failsValidation(
      Osoite(postinumeroKoodiUri = Some("posti_99999#2")),
      Tallennettu,
      Seq(ValidationError("path.postinumeroKoodiUri", invalidPostiosoiteKoodiUri("posti_99999#2")))
    )
  }

  it should "fail if values missing from julkaistu Osoite" in {
    failsValidation(
      Osoite(osoite = vainSuomeksi, postinumeroKoodiUri = None),
      Julkaistu,
      Seq(ValidationError("path.osoite", kielistettyWoSvenska), ValidationError("path.postinumeroKoodiUri", missingMsg))
    )
  }

  def failsValidation(
      e: Valintakoe,
      tila: Julkaisutila,
      allowedIds: Seq[UUID] = Seq(),
      crudOperation: CrudOperation = create,
      expected: Seq[ValidationError]
  ): Assertion =
    e.validate(
      "path",
      Some(e),
      ValidationContext(tila, kielet, crudOperation),
      allowedIds,
      koodistoClient.koodiUriExistsInKoodisto(ValintakoeTyyppiKoodisto, _),
      koodistoClient.koodiUriExistsInKoodisto(PostiosoiteKoodisto, _)
    ) match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors   => errors should contain theSameElementsAs expected
    }

  def passValidation(
      e: Valintakoe,
      oldE: Option[Valintakoe],
      tila: Julkaisutila,
      crudOperation: CrudOperation = create,
      allowedIds: Seq[UUID] = Seq()
  ): Assertion =
    e.validate(
      "path",
      oldE,
      ValidationContext(tila, kielet, crudOperation),
      allowedIds,
      koodistoClient.koodiUriExistsInKoodisto(ValintakoeTyyppiKoodisto, _),
      koodistoClient.koodiUriExistsInKoodisto(PostiosoiteKoodisto, _)
    ) match {
      case NoErrors => succeed
      case errors   => fail("Expected no errors, but received: " + errors)
    }

  "Valintakoe validation" should "succeed when julkaistu valintakoe without ennakkovalmistelut nor erityisjärjestelyt" in {
    val validKielistetty = Map(Fi -> "suomeksi", Sv -> "på svenska")
    val koe = Valintakoe(
      nimi = validKielistetty,
      metadata = Some(
        ValintakoeMetadata(
          tietoja = validKielistetty,
          vahimmaispisteet = Some(6.5),
          ohjeetEnnakkovalmistautumiseen = Map(),
          ohjeetErityisjarjestelyihin = Map()
        )
      )
    )
    passValidation(
      koe,
      Some(koe),
      Julkaistu,
      create
    )
  }

  it should "succeed if tyyppiKoodiUri not changed in modify operation" in {
    passValidation(Valintakoe(tyyppiKoodiUri = Some("valintakokeentyyppi_99#1")), None, Tallennettu, update)
  }

  it should "succeed when correct ID in modify operation" in {
    val id = UUID.randomUUID()
    passValidation(Valintakoe(id = Some(id)), None, Tallennettu, update, Seq(id))
  }

  it should "fail if id given for new valintakoe" in {
    val id = Some(UUID.randomUUID())
    failsValidation(Valintakoe(id = id), Tallennettu, expected = Seq(ValidationError("path.id", notMissingMsg(id))))
  }

  it should "fail if unknown id in modified valintakoe" in {
    val id = Some(UUID.randomUUID())
    failsValidation(
      Valintakoe(id = id),
      Tallennettu,
      Seq(UUID.randomUUID()),
      update,
      Seq(ValidationError("path.id", unknownValintakoeId(id.get.toString)))
    )
  }

  it should "fail if invalid tyyppiKoodiUri" in {
    failsValidation(
      Valintakoe(tyyppiKoodiUri = Some("valintakokeentyyppi_99#1")),
      Tallennettu,
      expected =
        Seq(ValidationError("path.tyyppiKoodiUri", invalidValintakoeTyyppiKoodiuri("valintakokeentyyppi_99#1")))
    )
  }

  it should "fail if values missing from julkaistu Valintakoe" in {
    failsValidation(
      Valintakoe(
        nimi = vainSuomeksi,
        metadata = Some(
          ValintakoeMetadata(
            tietoja = vainSuomeksi,
            vahimmaispisteet = Some(-1.5),
            liittyyEnnakkovalmistautumista = Some(true),
            erityisjarjestelytMahdollisia = Some(true),
            ohjeetEnnakkovalmistautumiseen = vainSuomeksi,
            ohjeetErityisjarjestelyihin = vainSuomeksi
          )
        )
      ),
      Julkaistu,
      expected = Seq(
        ValidationError("path.metadata.tietoja", kielistettyWoSvenska),
        ValidationError("path.metadata.vahimmaispisteet", notNegativeMsg),
        ValidationError("path.metadata.ohjeetEnnakkovalmistautumiseen", kielistettyWoSvenska),
        ValidationError("path.metadata.ohjeetErityisjarjestelyihin", kielistettyWoSvenska),
        ValidationError("path.nimi", kielistettyWoSvenska)
      )
    )
  }

  def failsValidation(
      e: Valintakoetilaisuus,
      tila: Julkaisutila,
      expected: Seq[ValidationError]
  ): Assertion =
    e.validate(
      "path",
      Some(e),
      ValidationContext(tila, kielet, create),
      koodistoClient.koodiUriExistsInKoodisto(PostiosoiteKoodisto, _)
    ) match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors   => errors should contain theSameElementsAs expected
    }

  "Valintakoetilaisuus validation" should "Succeed if postinumeroKoodiUri not changed in modify operation" in {
    Valintakoetilaisuus(osoite = Some(Osoite(postinumeroKoodiUri = Some("posti_99999#2")))).validate(
      "path",
      None,
      ValidationContext(Tallennettu, kielet, create),
      koodistoClient.koodiUriExistsInKoodisto(PostiosoiteKoodisto, _)
    ) match {
      case NoErrors =>
      case errors   => fail("Expected no errors, but received: " + errors)
    }
  }

  it should "fail if invalid osoite" in {
    failsValidation(
      Valintakoetilaisuus(osoite = Some(Osoite(postinumeroKoodiUri = Some("puppu")))),
      Tallennettu,
      Seq(ValidationError("path.osoite.postinumeroKoodiUri", invalidPostiosoiteKoodiUri("puppu")))
    )
  }

  it should "fail if invalid ajanjakso" in {
    val ajanjakso = Ajanjakso(inFuture(), Some(inPast()))
    failsValidation(
      Valintakoetilaisuus(aika = Some(ajanjakso), osoite = None),
      Tallennettu,
      Seq(ValidationError("path.aika", invalidAjanjaksoMsg(ajanjakso)))
    )
  }

  it should "fail if values missing from julkaistu Valintakoetilaisuus" in {
    failsValidation(
      Valintakoetilaisuus(osoite = None, aika = None, jarjestamispaikka = vainSuomeksi, lisatietoja = vainSuomeksi),
      Julkaistu,
      Seq(
        ValidationError("path.osoite", missingMsg),
        ValidationError("path.aika", missingMsg),
        ValidationError("path.jarjestamispaikka", kielistettyWoSvenska),
        ValidationError("path.lisatietoja", kielistettyWoSvenska)
      )
    )
  }

  def failsValidation(
      e: Aloituspaikat,
      tila: Julkaisutila,
      expected: Seq[ValidationError]
  ): Assertion =
    e.validate(ValidationContext(tila, kielet, create), "path") match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors   => errors should contain theSameElementsAs expected
    }

  "Aloituspaikat validation" should "fail if negative numbers for lukumäärät" in {
    failsValidation(
      Aloituspaikat(lukumaara = Some(-10), ensikertalaisille = Some(-5), kuvaus = Map()),
      Julkaistu,
      Seq(
        ValidationError("path.ensikertalaisille", notNegativeMsg),
        ValidationError("path.lukumaara", notNegativeMsg)
      )
    )
  }

  it should "fail if values missing from julkaistu Aloituspaikat" in {
    failsValidation(
      Aloituspaikat(lukumaara = None, kuvaus = vainSuomeksi),
      Julkaistu,
      Seq(
        ValidationError("path.lukumaara", missingMsg),
        ValidationError("path.kuvaus", kielistettyWoSvenska)
      )
    )
  }

  def failsValidation(
      e: Yhteyshenkilo,
      tila: Julkaisutila,
      expected: Seq[ValidationError]
  ): Assertion =
    e.validate(ValidationContext(tila, kielet, create), "path") match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors   => errors should contain theSameElementsAs expected
    }

  "Yhteyshenkilö validation" should "fail if missing or invalid values in julkaistu yhteyshenkilö" in {
    failsValidation(
      Yhteyshenkilo(Map(), vainSuomeksi, vainSuomeksi, vainSuomeksi, vainSuomeksi),
      Julkaistu,
      Seq(
        ValidationError("path.nimi", invalidKielistetty(kielet)),
        ValidationError("path.titteli", kielistettyWoSvenska),
        ValidationError("path.sahkoposti", kielistettyWoSvenska),
        ValidationError("path.puhelinnumero", kielistettyWoSvenska),
        ValidationError("path.wwwSivu", kielistettyWoSvenska),
        ValidationError("path.wwwSivu.fi", invalidUrl("vain suomeksi")),
        ValidationError("path.wwwSivu.sv", invalidUrl(""))
      )
    )
  }

  "Hakulomake validation" should "fail if missing or irrelevant values for MuuHakulomake" in {
    validateHakulomake(
      Some(MuuHakulomake),
      Some(ataruId),
      fullKielistetty,
      Map(Fi -> "", Sv -> "puppu"),
      kielet
    ) should equal(
      Seq(
        ValidationError("hakulomakeAtaruId", notMissingMsg(Some(ataruId))),
        ValidationError("hakulomakeKuvaus", notEmptyMsg),
        ValidationError("hakulomakeLinkki", invalidKielistetty(Seq(Fi))),
        ValidationError("hakulomakeLinkki", invalidUrl("")),
        ValidationError("hakulomakeLinkki", invalidUrl("puppu"))
      )
    )
  }

  it should "fail if missing or irrelevant values for Ataru" in {
    validateHakulomake(Some(Ataru), None, fullKielistetty, fullKielistetty, kielet) should equal(
      Seq(
        ValidationError("hakulomakeAtaruId", missingMsg),
        ValidationError("hakulomakeKuvaus", notEmptyMsg),
        ValidationError("hakulomakeLinkki", notEmptyMsg)
      )
    )
  }

  it should "fail if missing or irrelevant values for EiSähköistä" in {
    validateHakulomake(Some(EiSähköistä), Some(ataruId), vainSuomeksi, fullKielistetty, kielet) should equal(
      Seq(
        ValidationError("hakulomakeAtaruId", notMissingMsg(Some(ataruId))),
        ValidationError("hakulomakeLinkki", notEmptyMsg),
        ValidationError("hakulomakeKuvaus", kielistettyWoSvenska)
      )
    )
  }

  it should "fail if values given even though type not defined" in {
    validateHakulomake(None, Some(ataruId), fullKielistetty, fullKielistetty, kielet) should equal(
      Seq(
        ValidationError("hakulomakeAtaruId", notEmptyAlthoughOtherEmptyMsg("hakulomaketyyppi")),
        ValidationError("hakulomakeKuvaus", notEmptyAlthoughOtherEmptyMsg("hakulomaketyyppi")),
        ValidationError("hakulomakeLinkki", notEmptyAlthoughOtherEmptyMsg("hakulomaketyyppi"))
      )
    )
  }
}
