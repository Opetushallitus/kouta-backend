package fi.oph.kouta.integration

import fi.oph.kouta.TestData._
import fi.oph.kouta.client.{HakemusPalveluClient, HakuKoodiClient}
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.domain._
import fi.oph.kouta.repository.HakukohdeDAO
import fi.oph.kouta.service.{HakuServiceValidation, KoutaValidationException, OrganisaatioService}
import fi.oph.kouta.validation.ExternalQueryResults.itemFound
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{BaseValidationSpec, ValidationError}
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers.contain
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.util.UUID
import scala.util.{Failure, Try}

class HakuServiceValidationSpec extends BaseValidationSpec[Haku] {
  val organisaatioService  = mock[OrganisaatioService]
  val hakuKoodiClient      = mock[HakuKoodiClient]
  val hakemusPalveluClient = mock[HakemusPalveluClient]
  val hakukohdeDao         = mock[HakukohdeDAO]

  val ataruId            = UUID.randomUUID()
  val hakuOid            = HakuOid("1.2.246.562.29.0000000001")
  val hakuOid2           = HakuOid("1.2.246.562.29.0000000002")
  val max                = JulkaistuHaku
  val maxWithOid         = max.copy(oid = Some(hakuOid))
  val min                = MinHaku
  val maxMetadata        = max.metadata.get
  val maxWithOidMetadata = maxWithOid.metadata.get

  override val validator   = new HakuServiceValidation(organisaatioService, hakuKoodiClient, hakemusPalveluClient, hakukohdeDao)
  val vainSuomeksi         = Map(Fi -> "vain suomeksi", Sv -> "")
  val fullKielistetty      = Map(Fi -> "suomeksi", Sv -> "på svenska")
  val kielistettyWoSvenska = invalidKielistetty(Seq(Sv))

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(hakuKoodiClient.hakutapaKoodiUriExists("hakutapa_01#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.hakutapaKoodiUriExists("hakutapa_02#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.hakutapaKoodiUriExists("hakutapa_03#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.hakutapaKoodiUriExists("hakutapa_04#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.haunkohdejoukkoKoodiUriExists("haunkohdejoukko_17#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.haunkohdejoukonTarkenneKoodiUriExists("haunkohdejoukontarkenne_1#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.kausiKoodiUriExists("kausi_k#1")).thenAnswer(itemFound)
    when(hakemusPalveluClient.isExistingAtaruId(ataruId)).thenAnswer(itemFound)

    when(hakukohdeDao.listByHakuOid(hakuOid, TilaFilter.onlyOlemassaolevat())).thenAnswer(Seq[HakukohdeListItem]())
  }

  def failsModifyValidation(haku: Haku, oldHaku: Haku, expected: Seq[ValidationError]): Assertion =
    Try(validator.withValidation(haku, Some(oldHaku))(hk => hk)) match {
      case Failure(exp: KoutaValidationException) => exp.errorMessages should contain theSameElementsAs expected
      case _                                      => fail("Expecting validation failure, but it succeeded")
    }

  "Haku validation" should "succeed when new valid haku" in {
    passValidation(max)
  }

  it should "succeed when new incomplete luonnos" in {
    passValidation(min)
  }

  it should "succeed when modifying existing haku" in {
    passValidation(maxWithOid, max)
  }

  it should "fail when invalid perustiedot" in {
    failValidation(
      max.copy(oid = Some(HakuOid("1.2.3"))),
      Seq(
        ValidationError("oid", validationMsg("1.2.3")),
        ValidationError("oid", notMissingMsg(Some(HakuOid("1.2.3"))))
      )
    )
    failValidation(min.copy(kielivalinta = Seq()), "kielivalinta", missingMsg)
    failValidation(min.copy(nimi = Map(Fi -> "nimi")), "nimi", invalidKielistetty(Seq(Sv)))
    failValidation(max.copy(nimi = Map(Fi -> "nimi", Sv -> "")), "nimi", invalidKielistetty(Seq(Sv)))
    failValidation(min.copy(organisaatioOid = OrganisaatioOid("1.2.3")), "organisaatioOid", validationMsg("1.2.3"))
    failValidation(min.copy(organisaatioOid = OrganisaatioOid("")), "organisaatioOid", validationMsg(""))
  }
  it should "fail when missing or invalid hakutapaKoodiUri" in {
    failValidation(max.copy(hakutapaKoodiUri = None), "hakutapaKoodiUri", missingMsg)
    failValidation(max.copy(hakutapaKoodiUri = Some("puppu")), "hakutapaKoodiUri", validationMsg("puppu"))
    failValidation(
      max.copy(hakutapaKoodiUri = Some("hakutapa_99#1")),
      "hakutapaKoodiUri",
      invalidHakutapaKoodiUri("hakutapa_99#1")
    )
  }

  it should "fail when missing or invalid kohdejoukkoKoodiUri" in {
    failValidation(max.copy(kohdejoukkoKoodiUri = None), "kohdejoukkoKoodiUri", missingMsg)
    failValidation(max.copy(kohdejoukkoKoodiUri = Some("puppu")), "kohdejoukkoKoodiUri", validationMsg("puppu"))
    failValidation(
      max.copy(kohdejoukkoKoodiUri = Some("haunkohdejoukko_99#1")),
      "kohdejoukkoKoodiUri",
      invalidHaunKohdejoukkoKoodiUri("haunkohdejoukko_99#1")
    )
  }

  it should "fail when invalid kohdejoukonTarkenneKoodiUri" in {
    failValidation(
      max.copy(kohdejoukonTarkenneKoodiUri = Some("puppu")),
      "kohdejoukonTarkenneKoodiUri",
      validationMsg("puppu")
    )
    failValidation(
      max.copy(kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_99#1")),
      "kohdejoukonTarkenneKoodiUri",
      invalidHaunKohdejoukonTarkenneKoodiUri("haunkohdejoukontarkenne_99#1")
    )
  }

  it should "fail when invalid hakuajat" in {
    val ajanJakso = Ajanjakso(inFuture(1000), Some(inFuture(800)))
    failValidation(max.copy(hakuajat = List(ajanJakso)), "hakuajat[0]", invalidAjanjaksoMsg(ajanJakso))
  }

  it should "fail if metadata missing from julkaistu haku" in {
    failValidation(max.copy(metadata = None), "metadata", missingMsg)
  }

  "Metadata validation" should "fail if invalid yhteyshenkilö" in {
    failValidation(
      max.copy(metadata = Some(maxMetadata.copy(yhteyshenkilot = List(Yhteystieto1.copy(nimi = Map()))))),
      "metadata.yhteyshenkilot[0].nimi",
      invalidKielistetty(Seq(Fi, Sv))
    )
  }

  it should "fail if invalid tulevaisuudenAikataulu" in {
    val ajanJakso = Ajanjakso(inFuture(1000), Some(inFuture(800)))
    failValidation(
      max.copy(metadata = Some(maxMetadata.copy(tulevaisuudenAikataulu = Seq(ajanJakso)))),
      "metadata.tulevaisuudenAikataulu[0]",
      invalidAjanjaksoMsg(ajanJakso)
    )
  }

  it should "fail if invalid koulutuksenAlkamiskausi" in {
    failValidation(
      max.copy(metadata =
        Some(
          maxMetadata.copy(koulutuksenAlkamiskausi =
            Some(maxMetadata.koulutuksenAlkamiskausi.get.copy(koulutuksenAlkamiskausiKoodiUri = Some("kausi_k#99")))
          )
        )
      ),
      "metadata.koulutuksenAlkamiskausi.koulutuksenAlkamiskausiKoodiUri",
      invalidKausiKoodiuri("kausi_k#99")
    )
  }

  it should "fail if koulutuksenAlkamiskausi not given in julkaistu yhteishaku" in {
    failValidation(
      max.copy(
        hakutapaKoodiUri = Some("hakutapa_01#1"),
        metadata = Some(maxMetadata.copy(koulutuksenAlkamiskausi = None))
      ), "metadata.koulutuksenAlkamiskausi", missingMsg
    )
  }

  it should "fail if hakulomaketyyppi missing from julkaistu haku while other hakulomake values given" in {
    failValidation(
      max.copy(hakulomaketyyppi = None),
      Seq(
        ValidationError("hakulomakeLinkki", notEmptyAlthoughOtherEmptyMsg("hakulomaketyyppi")),
        ValidationError("hakulomaketyyppi", missingMsg)
      )
    )
  }

  "Haku validation in modify operation" should "succeed when koodiUris not changed, eventhough unknown koodiUri" in {
    val haku = maxWithOid.copy(
      hakutapaKoodiUri = Some("hakutapa_99#99"),
      kohdejoukkoKoodiUri = Some("haunkohdejoukko_99#99"),
      kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_99#99"),
      metadata = Some(
        maxMetadata.copy(koulutuksenAlkamiskausi =
          Some(maxMetadata.koulutuksenAlkamiskausi.get.copy(koulutuksenAlkamiskausiKoodiUri = Some("kausi_k#99")))
        )
      )
    )
    passValidation(haku, haku)
  }

  it should "succeed when ataruId not changed, eventhough unknown id" in {
    val hakuWithUnknownId = maxWithOid.copy(
      hakulomaketyyppi = Some(Ataru),
      hakulomakeLinkki = Map(),
      hakulomakeAtaruId = Some(UUID.randomUUID())
    )
    passValidation(hakuWithUnknownId, hakuWithUnknownId)
  }

  "Validate on julkaisu" should "succeed when jatkuva haku" in {
    passValidation(
      maxWithOid.copy(hakutapaKoodiUri = Some("hakutapa_03#1"), hakuajat = List(Ajanjakso(inFuture()))),
      max.copy(tila = Tallennettu)
    )
    passValidation(
      maxWithOid
        .copy(hakutapaKoodiUri = Some("hakutapa_03#1"), hakuajat = List(Ajanjakso(inFuture(), Some(inFuture(1000))))),
      max.copy(tila = Tallennettu)
    )
  }

  it should "succeed when joustava haku" in {
    passValidation(
      maxWithOid.copy(hakutapaKoodiUri = Some("hakutapa_04#1"), hakuajat = List(Ajanjakso(inFuture()))),
      max.copy(tila = Tallennettu)
    )
    passValidation(
      maxWithOid
        .copy(hakutapaKoodiUri = Some("hakutapa_04#1"), hakuajat = List(Ajanjakso(inFuture(), Some(inFuture(1000))))),
      max.copy(tila = Tallennettu)
    )
  }

  it should "succeed when other than jatkuva haku" in {
    passValidation(
      maxWithOid
        .copy(hakutapaKoodiUri = Some("hakutapa_02#1"), hakuajat = List(Ajanjakso(inFuture(), Some(inFuture(1000))))),
      max.copy(tila = Tallennettu)
    )
  }

  it should "fail when end of hakuaika not given and hakutapa not jatkuva nor joustava" in {
    val ajanjakso = Ajanjakso(inFuture())
    failValidation(
      max.copy(hakutapaKoodiUri = Some("hakutapa_02#1"), hakuajat = List(ajanjakso)),
      "hakuajat[0].paattyy",
      missingMsg
    )
  }

  it should "fail when end of hakuaika not in future" in {
    val paattyy = inPast(500)
    failValidation(
      max.copy(hakutapaKoodiUri = Some("hakutapa_02#1"), hakuajat = List(Ajanjakso(inPast(1000), Some(paattyy)))),
      "hakuajat[0].paattyy",
      pastDateMsg(paattyy)
    )
    failValidation(
      max.copy(hakutapaKoodiUri = Some("hakutapa_03#1"), hakuajat = List(Ajanjakso(inPast(1000), Some(paattyy)))),
      "hakuajat[0].paattyy",
      pastDateMsg(paattyy)
    )
  }

  it should "fail when end of tulevaisuudenAikataulu missing or not in future" in {
    failValidation(
      max.copy(metadata = Some(maxMetadata.copy(tulevaisuudenAikataulu = Seq(Ajanjakso(inFuture()))))),
      "metadata.tulevaisuudenAikataulu[0].paattyy",
      missingMsg
    )
    val paattyy = inPast(500)
    failValidation(
      max.copy(metadata = Some(maxMetadata.copy(tulevaisuudenAikataulu = Seq(Ajanjakso(inPast(1000), Some(paattyy)))))),
      "metadata.tulevaisuudenAikataulu[0].paattyy",
      pastDateMsg(paattyy)
    )
  }

  it should "fail when koulutuksenAlkamiskausi not in future" in {
    val alkaa        = inPast(1000)
    val paattyy      = inPast(500)
    val alkamisvuosi = "2020"
    val alkamiskausi = Some(
      maxMetadata.koulutuksenAlkamiskausi.get.copy(
        koulutuksenAlkamisvuosi = Some(alkamisvuosi),
        koulutuksenAlkamispaivamaara = Some(alkaa),
        koulutuksenPaattymispaivamaara = Some(paattyy)
      )
    )

    failValidation(
      max.copy(metadata =
        Some(
          maxMetadata.copy(
            koulutuksenAlkamiskausi = alkamiskausi
          )
        )
      ),
      Seq(
        ValidationError("metadata.koulutuksenAlkamiskausi.koulutuksenAlkamisvuosi", pastDateMsg(alkamisvuosi)),
        ValidationError("metadata.koulutuksenAlkamiskausi.koulutuksenAlkamispaivamaara", pastDateMsg(alkaa)),
        ValidationError("metadata.koulutuksenAlkamiskausi.koulutuksenPaattymispaivamaara", pastDateMsg(paattyy))
      )
    )
  }

  "State change" should "succeed from tallennettu to julkaistu" in {
    passValidation(maxWithOid, maxWithOid.copy(tila = Tallennettu))
  }

  it should "succeed from julkaistu to arkistoitu" in {
    passValidation(maxWithOid.copy(tila = Arkistoitu), maxWithOid)
  }

  it should "succeed from arkistoitu to julkaistu" in {
    passValidation(maxWithOid, maxWithOid.copy(tila = Arkistoitu))
  }

  it should "succeed from julkaistu to tallennettu" in {
    passValidation(maxWithOid.copy(tila = Tallennettu), maxWithOid)
  }

  it should "succeed from tallennettu to poistettu" in {
    passValidation(maxWithOid.copy(tila = Poistettu), maxWithOid.copy(tila = Tallennettu))
  }

  def failStageChangeValidation(newTila: Julkaisutila, oldTila: Julkaisutila): Assertion =
    Try(
      validator.withValidation(maxWithOid.copy(tila = newTila), Some(maxWithOid.copy(tila = oldTila)))(e => e)
    ) match {
      case Failure(exp: KoutaValidationException) =>
        exp.errorMessages should contain theSameElementsAs Seq(
          ValidationError("tila", illegalStateChange("haulle", oldTila, newTila))
        )
      case _ => fail("Expecting illegalStateChange, but it succeeded")
    }

  it should "fail from tallennettu to arkistoitu" in {
    failStageChangeValidation(Arkistoitu, Tallennettu)
  }

  it should "fail from arkistoitu to tallennettu" in {
    failStageChangeValidation(Tallennettu, Arkistoitu)
  }

  it should "fail from julkaistu to poistettu" in {
    failStageChangeValidation(Poistettu, Julkaistu)
  }

  it should "fail from arkistoitu to poistettu" in {
    failStageChangeValidation(Poistettu, Arkistoitu)
  }

  it should "fail from poistettu to tallennettu" in {
    failStageChangeValidation(Tallennettu, Poistettu)
  }

  it should "fail from tallennettu to poistettu when existing hakukohteet" in {
    val haku = max.copy(oid = Some(hakuOid2))
    when(hakukohdeDao.listByHakuOid(hakuOid2, TilaFilter.onlyOlemassaolevat())).thenAnswer(Seq(MinHakukohdeListItem))
    failsModifyValidation(
      haku.copy(tila = Poistettu),
      haku.copy(tila = Tallennettu),
      Seq(
        ValidationError("tila", integrityViolationMsg("Hakua", "hakukohteita"))
      )
    )
  }
}
