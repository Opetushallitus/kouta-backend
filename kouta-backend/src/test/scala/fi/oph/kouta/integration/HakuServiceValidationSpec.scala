package fi.oph.kouta.integration

import fi.oph.kouta.TestData._
import fi.oph.kouta.client.{HakemusPalveluClient, HakuKoodiClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.repository.HakukohdeDAO
import fi.oph.kouta.service.{HakuServiceValidation, OrganisaatioService}
import fi.oph.kouta.validation.ExternalQueryResults.itemFound
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{BaseServiceValidationSpec, ValidationError}
import org.scalatest.Assertion

import java.util.UUID

class HakuServiceValidationSpec extends BaseServiceValidationSpec[Haku] {
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

  override val validator =
    new HakuServiceValidation(hakuKoodiClient, hakemusPalveluClient, hakukohdeDao)

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(hakuKoodiClient.koodiUriExistsInKoodisto(HakutapaKoodisto, "hakutapa_01#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.koodiUriExistsInKoodisto(HakutapaKoodisto, "hakutapa_02#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.koodiUriExistsInKoodisto(HakutapaKoodisto, "hakutapa_03#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.koodiUriExistsInKoodisto(HakutapaKoodisto, "hakutapa_04#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.haunkohdejoukkoKoodiUriExists("haunkohdejoukko_17#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.haunkohdejoukonTarkenneKoodiUriExists("haunkohdejoukontarkenne_1#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.koodiUriExistsInKoodisto(KausiKoodisto, "kausi_k#1")).thenAnswer(itemFound)
    when(hakemusPalveluClient.isExistingAtaruIdFromCache(ataruId)).thenAnswer(itemFound)

    when(hakukohdeDao.listByHakuOid(hakuOid, TilaFilter.onlyOlemassaolevat())).thenAnswer(Seq[HakukohdeListItem]())
  }

  "Haku validation" should "succeed when new valid haku" in {
    passesValidation(max)
  }

  it should "succeed when new incomplete luonnos" in {
    passesValidation(min)
  }

  it should "succeed when modifying existing haku" in {
    passesValidation(maxWithOid, max)
  }

  it should "fail when invalid perustiedot" in {
    failsValidation(
      max.copy(oid = Some(HakuOid("1.2.3"))),
      Seq(
        ValidationError("oid", validationMsg("1.2.3")),
        ValidationError("oid", notMissingMsg(Some(HakuOid("1.2.3"))))
      )
    )
    failsValidation(min.copy(nimi = Map(), kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(min.copy(nimi = Map(Fi -> "nimi")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(max.copy(nimi = Map(Fi -> "nimi", Sv -> "")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(min.copy(organisaatioOid = OrganisaatioOid("1.2.3")), "organisaatioOid", validationMsg("1.2.3"))
    failsValidation(min.copy(organisaatioOid = OrganisaatioOid("")), "organisaatioOid", validationMsg(""))
  }
  it should "fail when missing or invalid hakutapaKoodiUri" in {
    failsValidation(max.copy(hakutapaKoodiUri = None), "hakutapaKoodiUri", missingMsg)
    failsValidation(
      max.copy(hakutapaKoodiUri = Some("hakutapa_99#1")),
      "hakutapaKoodiUri",
      invalidHakutapaKoodiUri("hakutapa_99#1")
    )
  }

  it should "fail when missing or invalid kohdejoukkoKoodiUri" in {
    failsValidation(max.copy(kohdejoukkoKoodiUri = None), "kohdejoukkoKoodiUri", missingMsg)
    failsValidation(
      max.copy(kohdejoukkoKoodiUri = Some("haunkohdejoukko_99#1")),
      "kohdejoukkoKoodiUri",
      invalidHaunKohdejoukkoKoodiUri("haunkohdejoukko_99#1")
    )
  }

  it should "fail when invalid kohdejoukonTarkenneKoodiUri" in {
    failsValidation(
      max.copy(kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_99#1")),
      "kohdejoukonTarkenneKoodiUri",
      invalidHaunKohdejoukonTarkenneKoodiUri("haunkohdejoukontarkenne_99#1")
    )
  }

  it should "fail when invalid hakuajat" in {
    val ajanJakso = Ajanjakso(inFuture(1000), Some(inFuture(800)))
    failsValidation(max.copy(hakuajat = List(ajanJakso)), "hakuajat[0]", invalidAjanjaksoMsg(ajanJakso))
  }

  it should "fail when given ataruid not found" in {
    val randomUUID = UUID.randomUUID()
    failsValidation(
      max.copy(
        hakulomaketyyppi = Some(Ataru),
        hakulomakeLinkki = Map(),
        hakulomakeAtaruId = Some(randomUUID)),
      Seq(
        ValidationError("hakulomakeAtaruId", unknownAtaruId(randomUUID)),
        ValidationError("hakulomakeAtaruId", invalidAtaruFormAllowsOnlyYhteishaku(randomUUID))
      )
    )
  }

  it should "fail if metadata missing from julkaistu haku" in {
    failsValidation(max.copy(metadata = None), "metadata", missingMsg)
  }

  "Metadata validation" should "fail if invalid yhteyshenkilö" in {
    failsValidation(
      max.copy(metadata = Some(maxMetadata.copy(yhteyshenkilot = List(Yhteystieto1.copy(nimi = Map()))))),
      "metadata.yhteyshenkilot[0].nimi",
      invalidKielistetty(Seq(Fi, Sv))
    )
  }

  it should "fail if invalid tulevaisuudenAikataulu" in {
    val ajanJakso = Ajanjakso(inFuture(1000), Some(inFuture(800)))
    failsValidation(
      max.copy(metadata = Some(maxMetadata.copy(tulevaisuudenAikataulu = Seq(ajanJakso)))),
      "metadata.tulevaisuudenAikataulu[0]",
      invalidAjanjaksoMsg(ajanJakso)
    )
  }

  it should "fail if invalid koulutuksenAlkamiskausi" in {
    failsValidation(
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
    failsValidation(
      max.copy(
        hakutapaKoodiUri = Some("hakutapa_01#1"),
        metadata = Some(maxMetadata.copy(koulutuksenAlkamiskausi = None))
      ),
      "metadata.koulutuksenAlkamiskausi",
      missingMsg
    )
  }

  it should "fail if hakulomaketyyppi missing from julkaistu haku while other hakulomake values given" in {
    failsValidation(
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
    passesValidation(haku, haku)
  }

  it should "succeed when ataruId not changed, eventhough unknown id" in {
    val hakuWithUnknownId = maxWithOid.copy(
      hakulomaketyyppi = Some(Ataru),
      hakulomakeLinkki = Map(),
      hakulomakeAtaruId = Some(UUID.randomUUID())
    )
    passesValidation(hakuWithUnknownId, hakuWithUnknownId)
  }

  "Validate on julkaisu" should "succeed when jatkuva haku" in {
    passesValidation(
      maxWithOid.copy(hakutapaKoodiUri = Some("hakutapa_03#1"), hakuajat = List(Ajanjakso(inFuture()))),
      max.copy(tila = Tallennettu)
    )
    passesValidation(
      maxWithOid
        .copy(hakutapaKoodiUri = Some("hakutapa_03#1"), hakuajat = List(Ajanjakso(inFuture(), Some(inFuture(1000))))),
      max.copy(tila = Tallennettu)
    )
  }

  it should "succeed when joustava haku" in {
    passesValidation(
      maxWithOid.copy(hakutapaKoodiUri = Some("hakutapa_04#1"), hakuajat = List(Ajanjakso(inFuture()))),
      max.copy(tila = Tallennettu)
    )
    passesValidation(
      maxWithOid
        .copy(hakutapaKoodiUri = Some("hakutapa_04#1"), hakuajat = List(Ajanjakso(inFuture(), Some(inFuture(1000))))),
      max.copy(tila = Tallennettu)
    )
  }

  it should "succeed when other than jatkuva haku" in {
    passesValidation(
      maxWithOid
        .copy(hakutapaKoodiUri = Some("hakutapa_02#1"), hakuajat = List(Ajanjakso(inFuture(), Some(inFuture(1000))))),
      max.copy(tila = Tallennettu)
    )
  }

  it should "fail when end of hakuaika not given and hakutapa not jatkuva nor joustava" in {
    val ajanjakso = Ajanjakso(inFuture())
    failsValidation(
      max.copy(hakutapaKoodiUri = Some("hakutapa_02#1"), hakuajat = List(ajanjakso)),
      "hakuajat[0].paattyy",
      missingMsg
    )
  }

  it should "fail when end of hakuaika not in future" in {
    failsValidation(
      max.copy(hakutapaKoodiUri = Some("hakutapa_02#1"), hakuajat = List(inPastJakso)),
      "hakuajat[0].paattyy",
      pastDateMsg(inPastAikaleima)
    )
    failsValidation(
      max.copy(hakutapaKoodiUri = Some("hakutapa_03#1"), hakuajat = List(inPastJakso)),
      "hakuajat[0].paattyy",
      pastDateMsg(inPastAikaleima)
    )
  }

  it should "fail when end of tulevaisuudenAikataulu missing or not in future" in {
    failsValidation(
      max.copy(metadata = Some(maxMetadata.copy(tulevaisuudenAikataulu = Seq(Ajanjakso(inFuture()))))),
      "metadata.tulevaisuudenAikataulu[0].paattyy",
      missingMsg
    )
    failsValidation(
      max.copy(metadata = Some(maxMetadata.copy(tulevaisuudenAikataulu = Seq(inPastJakso)))),
      "metadata.tulevaisuudenAikataulu[0].paattyy",
      pastDateMsg(inPastAikaleima)
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

    failsValidation(
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

  it should "succeed when state changes from arkistoitu to julkaistu, eventhough timestamps not in future" in {
    passesValidation(
      maxWithOid.copy(
        hakuajat = List(inPastJakso),
        metadata = Some(
          maxMetadata.copy(
            tulevaisuudenAikataulu = Seq(inPastJakso),
            koulutuksenAlkamiskausi = Some(inPastKoulutuksenAlkamiskausi)
          )
        )
      ),
      maxWithOid.copy(tila = Arkistoitu)
    )
  }

  "State change" should "succeed from tallennettu to julkaistu" in {
    passesValidation(maxWithOid, maxWithOid.copy(tila = Tallennettu))
  }

  it should "succeed from julkaistu to arkistoitu" in {
    passesValidation(maxWithOid.copy(tila = Arkistoitu), maxWithOid)
  }

  it should "succeed from arkistoitu to julkaistu" in {
    passesValidation(maxWithOid, maxWithOid.copy(tila = Arkistoitu))
  }

  it should "succeed from julkaistu to tallennettu" in {
    passesValidation(maxWithOid.copy(tila = Tallennettu), maxWithOid)
  }

  it should "succeed from tallennettu to poistettu" in {
    passesValidation(maxWithOid.copy(tila = Poistettu), maxWithOid.copy(tila = Tallennettu))
  }

  def failStageChangeValidation(newTila: Julkaisutila, oldTila: Julkaisutila): Assertion =
    failsStageChangeValidation(
      maxWithOid.copy(tila = newTila),
      maxWithOid.copy(tila = oldTila),
      illegalStateChange("haulle", oldTila, newTila)
    )

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
