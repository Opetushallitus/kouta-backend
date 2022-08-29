package fi.oph.kouta.integration

import fi.oph.kouta.TestData.{inFuture, inPast}
import fi.oph.kouta.client.{HakemusPalveluClient, HakuKoodiClient, KoulutusKoodiClient}
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
  val hakuKoodiClient      = mock[HakuKoodiClient]
  val koulutusKoodiClient  = mock[KoulutusKoodiClient]
  val hakemusPalveluClient = mock[HakemusPalveluClient]

  val vainSuomeksi         = Map(Fi -> "vain suomeksi", Sv -> "")
  val kielistettyWoSvenska = invalidKielistetty(Seq(Sv))
  val kielet               = Seq(Fi, Sv)

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(hakuKoodiClient.postiosoitekoodiExists("posti_04230#2")).thenAnswer(itemFound)
  }

  def failsValidation(
      e: KoulutuksenAlkamiskausi,
      tila: Julkaisutila,
      expected: Seq[ValidationError]
  ): Assertion =
    e.validate(tila, Seq(Fi, Sv), "path", Some(e), new ValidationContext(), hakuKoodiClient.kausiKoodiUriExists) match {
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
    ).validate(Tallennettu, kielet, "path", None, new ValidationContext(), hakuKoodiClient.kausiKoodiUriExists) match {
      case NoErrors =>
      case errors   => fail("Expected no errors, but received: " + errors)
    }
  }

  it should "fail if invalid AlkamiskausiKoodiUri" in {
    failsValidation(
      KoulutuksenAlkamiskausi(
        alkamiskausityyppi = Some(TarkkaAlkamisajankohta),
        koulutuksenAlkamiskausiKoodiUri = Some("puppu")
      ),
      Tallennettu,
      Seq(ValidationError("path.koulutuksenAlkamiskausiKoodiUri", validationMsg("puppu")))
    )
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

  def failsValidation(
      e: Osoite,
      tila: Julkaisutila,
      expected: Seq[ValidationError]
  ): Assertion =
    e.validate(
      tila,
      kielet,
      "path",
      Some(e),
      new ValidationContext(),
      hakuKoodiClient.postiosoitekoodiExists
    ) match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors   => errors should contain theSameElementsAs expected
    }

  "Osoite validation" should "Succeed if postinumeroKoodiUri not changed in modify operation" in {
    Osoite(postinumeroKoodiUri = Some("posti_99999#2")).validate(
      Tallennettu,
      kielet,
      "path",
      None,
      new ValidationContext(),
      hakuKoodiClient.postiosoitekoodiExists
    ) match {
      case NoErrors =>
      case errors   => fail("Expected no errors, but received: " + errors)
    }
  }

  it should "fail if invalid postinumeroKoodiUri" in {
    failsValidation(
      Osoite(postinumeroKoodiUri = Some("puppu")),
      Tallennettu,
      Seq(ValidationError("path.postinumeroKoodiUri", validationMsg("puppu")))
    )
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
      expected: Seq[ValidationError],
      crudOperation: CrudOperation = create
  ): Assertion =
    e.validate(
      tila,
      kielet,
      "path",
      Some(e),
      crudOperation,
      new ValidationContext(),
      hakuKoodiClient.valintakoeTyyppiKoodiUriExists,
      hakuKoodiClient.postiosoitekoodiExists
    ) match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors   => errors should contain theSameElementsAs expected
    }

  def passValidation(
      e: Valintakoe,
      tila: Julkaisutila,
      crudOperation: CrudOperation = create
  ): Assertion =
    e.validate(
      tila,
      kielet,
      "path",
      None,
      crudOperation,
      new ValidationContext(),
      hakuKoodiClient.valintakoeTyyppiKoodiUriExists,
      hakuKoodiClient.postiosoitekoodiExists
    ) match {
      case NoErrors => succeed
      case errors   => fail("Expected no errors, but received: " + errors)
    }

  "Valintakoe validation" should "succeed when julkaistu valintakoe without ennakkovalmistelut nor erityisjärjestelyt" in {
    val validKielistetty = Map(Fi -> "suomeksi", Sv -> "på svenska")
    passValidation(
      Valintakoe(
        nimi = validKielistetty,
        metadata = Some(
          ValintakoeMetadata(
            tietoja = validKielistetty,
            vahimmaispisteet = Some(6.5),
            ohjeetEnnakkovalmistautumiseen = Map(),
            ohjeetErityisjarjestelyihin = Map()
          )
        )
      ),
      Julkaistu,
      create
    )
  }

  it should "succeed if tyyppiKoodiUri not changed in modify operation" in {
    passValidation(Valintakoe(tyyppiKoodiUri = Some("valintakokeentyyppi_99#1")), Tallennettu, update)
  }

  it should "fail if id given for new valintakoe" in {
    val id = Some(UUID.randomUUID())
    failsValidation(Valintakoe(id = id), Tallennettu, Seq(ValidationError("path.id", notMissingMsg(id))))
  }

  it should "fail if invalid tyyppiKoodiUri" in {
    failsValidation(
      Valintakoe(tyyppiKoodiUri = Some("puppu")),
      Tallennettu,
      Seq(ValidationError("path.tyyppiKoodiUri", validationMsg("puppu")))
    )
    failsValidation(
      Valintakoe(tyyppiKoodiUri = Some("valintakokeentyyppi_99#1")),
      Tallennettu,
      Seq(ValidationError("path.tyyppiKoodiUri", invalidValintakoeTyyppiKooriuri("valintakokeentyyppi_99#1")))
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
      Seq(
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
      tila,
      Seq(Fi, Sv),
      "path",
      Some(e),
      new ValidationContext(),
      hakuKoodiClient.postiosoitekoodiExists
    ) match {
      case NoErrors => fail("Expecting validation failure, but it succeeded")
      case errors   => errors should contain theSameElementsAs expected
    }

  "Valintakoetilaisuus validation" should "Succeed if postinumeroKoodiUri not changed in modify operation" in {
    Valintakoetilaisuus(osoite = Some(Osoite(postinumeroKoodiUri = Some("posti_99999#2")))).validate(
      Tallennettu,
      kielet,
      "path",
      None,
      new ValidationContext(),
      hakuKoodiClient.postiosoitekoodiExists
    ) match {
      case NoErrors =>
      case errors   => fail("Expected no errors, but received: " + errors)
    }
  }

  it should "fail if invalid osoite" in {
    failsValidation(
      Valintakoetilaisuus(osoite = Some(Osoite(postinumeroKoodiUri = Some("puppu")))),
      Tallennettu,
      Seq(ValidationError("path.osoite.postinumeroKoodiUri", validationMsg("puppu")))
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
    e.validate(tila, Seq(Fi, Sv), "path") match {
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

  private def doAssertKoodistoQuery(validationContext: ValidationContext = new ValidationContext()): IsValid =
    assertKoodistoQueryResult(
      "valintakokeentyyppi_1#1",
      hakuKoodiClient.valintakoeTyyppiKoodiUriExists,
      "path",
      validationContext,
      invalidValintakoeTyyppiKooriuri("valintakokeentyyppi_1#1")
    )

  "Koodisto validation" should "succeed when valid koodiUri" in {
    when(hakuKoodiClient.valintakoeTyyppiKoodiUriExists("valintakokeentyyppi_1#1")).thenAnswer(itemFound)
    doAssertKoodistoQuery() should equal(NoErrors)
  }

  it should "fail when invalid koodiUri" in {
    when(hakuKoodiClient.valintakoeTyyppiKoodiUriExists("valintakokeentyyppi_1#1")).thenAnswer(itemNotFound)
    doAssertKoodistoQuery() should equal(error("path", invalidValintakoeTyyppiKooriuri("valintakokeentyyppi_1#1")))
  }

  it should "fail when koodiUri query failed" in {
    when(hakuKoodiClient.valintakoeTyyppiKoodiUriExists("valintakokeentyyppi_1#1")).thenAnswer(queryFailed)
    val validationContext = new ValidationContext()
    doAssertKoodistoQuery(validationContext) should equal(error("path", koodistoServiceFailureMsg))
    validationContext.isKoodistoServiceOk() should equal(false)
  }

  it should "fail when koodisto-service failure has been detected already before" in {
    when(hakuKoodiClient.valintakoeTyyppiKoodiUriExists("valintakokeentyyppi_1#1")).thenAnswer(itemFound)
    val validationContext = new ValidationContext()
    validationContext.setKoodistoServiceOk(false)
    doAssertKoodistoQuery(validationContext) should equal(error("path", koodistoServiceFailureMsg))
  }

  private def doAssertKoulutustyyppiKoodistoQuery(
      validationContext: ValidationContext = new ValidationContext()
  ): IsValid =
    assertKoulutustyyppiQueryResult(
      "koulutus_371101#1",
      ammatillisetKoulutustyypit,
      koulutusKoodiClient,
      "path",
      validationContext,
      invalidKoulutuskoodiuri("koulutus_371101#1")
    )

  "Koulutustyyppi-koodisto validation" should "succeed when valid koulutusKoodiUri for koulutustyyppi-list" in {
    when(koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExist(ammatillisetKoulutustyypit, "koulutus_371101#1"))
      .thenAnswer(itemFound)
    doAssertKoulutustyyppiKoodistoQuery() should equal(NoErrors)
  }

  it should "fail when invalid koulutusKoodiUri for koulutustyyppi-list" in {
    when(koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExist(ammatillisetKoulutustyypit, "koulutus_371101#1"))
      .thenAnswer(itemNotFound)
    doAssertKoulutustyyppiKoodistoQuery() should equal(error("path", invalidKoulutuskoodiuri("koulutus_371101#1")))
  }

  it should "fail when koodisto-query failed" in {
    when(koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExist(ammatillisetKoulutustyypit, "koulutus_371101#1"))
      .thenAnswer(queryFailed)
    val validationContext = new ValidationContext()
    doAssertKoulutustyyppiKoodistoQuery(validationContext) should equal(error("path", koodistoServiceFailureMsg))
    validationContext.isKoodistoServiceOk() should equal(false)
  }

  it should "fail when koodisto-service failure has been detected already before" in {
    when(koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExist(ammatillisetKoulutustyypit, "koulutus_371101#1"))
      .thenAnswer(itemFound)
    val validationContext = new ValidationContext()
    validationContext.setKoodistoServiceOk(false)
    doAssertKoulutustyyppiKoodistoQuery(validationContext) should equal(error("path", koodistoServiceFailureMsg))
  }

  private def doAssertKoulutusKoodiUriQuery(validationContext: ValidationContext = new ValidationContext()): IsValid =
    assertKoulutuskoodiQueryResult(
      "koulutus_301104#1",
      lukioKoulutusKoodiUrit,
      koulutusKoodiClient,
      "path",
      validationContext,
      invalidKoulutuskoodiuri("koulutus_301104#1")
    )

  "KoulutusKoodiUri-validation" should "succeed when valid koulutusKoodiUri for filter-list" in {
    when(koulutusKoodiClient.koulutusKoodiUriExists(lukioKoulutusKoodiUrit, "koulutus_301104#1"))
      .thenAnswer(itemFound)
    doAssertKoulutusKoodiUriQuery() should equal(NoErrors)
  }

  it should "fail when invalid koulutusKoodiUri for filter-list" in {
    when(koulutusKoodiClient.koulutusKoodiUriExists(lukioKoulutusKoodiUrit, "koulutus_301104#1"))
      .thenAnswer(itemNotFound)
    doAssertKoulutusKoodiUriQuery() should equal(error("path", invalidKoulutuskoodiuri("koulutus_301104#1")))
  }

  it should "fail when koodisto-query failed" in {
    when(koulutusKoodiClient.koulutusKoodiUriExists(lukioKoulutusKoodiUrit, "koulutus_301104#1"))
      .thenAnswer(queryFailed)
    val validationContext = new ValidationContext()
    doAssertKoulutusKoodiUriQuery(validationContext) should equal(error("path", koodistoServiceFailureMsg))
    validationContext.isKoodistoServiceOk() should equal(false)
  }

  it should "fail when koodisto-service failure has been detected already before" in {
    when(koulutusKoodiClient.koulutusKoodiUriExists(lukioKoulutusKoodiUrit, "koulutus_301104#1"))
      .thenAnswer(itemFound)
    val validationContext = new ValidationContext()
    validationContext.setKoodistoServiceOk(false)
    doAssertKoulutusKoodiUriQuery(validationContext) should equal(error("path", koodistoServiceFailureMsg))
  }

  private val ataruId = UUID.randomUUID()

  private def doAssertAtaruQuery(validationContext: ValidationContext = new ValidationContext()): IsValid =
    assertAtaruQueryResult(ataruId, hakemusPalveluClient, "path", validationContext, unknownAtaruId(ataruId))

  "AtaruId-validation" should "succeed when valid ataruId" in {
    when(hakemusPalveluClient.isExistingAtaruId(ataruId)).thenAnswer(itemFound)
    doAssertAtaruQuery() should equal(NoErrors)
  }

  it should "fail when invalid ataruId" in {
    when(hakemusPalveluClient.isExistingAtaruId(ataruId)).thenAnswer(itemNotFound)
    doAssertAtaruQuery() should equal(error("path", unknownAtaruId(ataruId)))
  }

  it should "fail when Ataru-query failed" in {
    when(hakemusPalveluClient.isExistingAtaruId(ataruId)).thenAnswer(queryFailed)
    val validationContext = new ValidationContext()
    doAssertAtaruQuery(validationContext) should equal(error("path", ataruServiceFailureMsg))
    validationContext.isAtaruServiceOk() should equal(false)
  }

  it should "fail when koodisto-service failure has been detected already before" in {
    when(hakemusPalveluClient.isExistingAtaruId(ataruId)).thenAnswer(itemFound)
    val validationContext = new ValidationContext()
    validationContext.setAtaruServiceOk(false)
    doAssertAtaruQuery(validationContext) should equal(error("path", ataruServiceFailureMsg))
  }
}
