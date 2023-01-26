package fi.oph.kouta.integration

import fi.oph.kouta.TestData.{MinHakukohdeListItem, MinYoValintaperuste, Valintakoe1, Valintatapa1, YoValintaperuste, inPast}
import fi.oph.kouta.client.HakuKoodiClient
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Ajanjakso, Amm, Arkistoitu, Column, Fi, GenericValintaperusteMetadata, HakukohdeListItem, HakutapaKoodisto, HaunKohdejoukkoKoodisto, Julkaistu, Julkaisutila, Poistettu, PostiosoiteKoodisto, Row, SisaltoTeksti, Sv, Tallennettu, Taulukko, TilaFilter, ValintakoeTyyppiKoodisto, Valintaperuste, Valintatapa, ValintatapaKoodisto}
import fi.oph.kouta.repository.HakukohdeDAO
import fi.oph.kouta.service.ValintaperusteServiceValidation
import fi.oph.kouta.validation.{BaseServiceValidationSpec, ValidationError}
import fi.oph.kouta.validation.ExternalQueryResults.itemFound
import fi.oph.kouta.validation.Validations.{InvalidMetadataTyyppi, illegalStateChange, integrityViolationMsg, invalidHakutapaKoodiUri, invalidHaunKohdejoukkoKoodiUri, invalidKielistetty, invalidValintakoeTyyppiKooriuri, invalidValintatapaKoodiUri, minmaxMsg, missingMsg, notMissingMsg, notModifiableMsg, notNegativeMsg, pastDateMsg, validationMsg}
import org.scalatest.Assertion

import java.util.UUID

class ValintaperusteServiceValidationSpec extends BaseServiceValidationSpec[Valintaperuste] {
  val hakuKoodiClient     = mock[HakuKoodiClient]
  val hakukohdeDao        = mock[HakukohdeDAO]

  val valintaperusteId  = UUID.randomUUID()
  val valintaperusteId2 = UUID.randomUUID()
  val max               = YoValintaperuste
  val min               = MinYoValintaperuste
  val maxWithId         = max.copy(id = Some(valintaperusteId))
  val maxMetadata       = max.metadata.get.asInstanceOf[GenericValintaperusteMetadata]

  private def maxWithValintatapa(tapa: Valintatapa): Valintaperuste =
    max.copy(metadata = Some(maxMetadata.copy(valintatavat = Seq(tapa))))

  private def maxWithKoeTilaisuudenAika(aika: Ajanjakso): Valintaperuste = {
    val tilaisuus = max.valintakokeet.head.tilaisuudet.head.copy(aika = Some(aika))
    max.copy(valintakokeet = List(max.valintakokeet.head.copy(tilaisuudet = List(tilaisuus))))
  }

  override val validator = new ValintaperusteServiceValidation(hakuKoodiClient, hakukohdeDao)

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(hakuKoodiClient.koodiUriExistsInKoodisto(HakutapaKoodisto, "hakutapa_03#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.koodiUriExistsInKoodisto(HaunKohdejoukkoKoodisto, "haunkohdejoukko_15#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.koodiUriExistsInKoodisto(PostiosoiteKoodisto, "posti_04230#2")).thenAnswer(itemFound)
    when(hakuKoodiClient.koodiUriExistsInKoodisto(ValintakoeTyyppiKoodisto, "valintakokeentyyppi_1#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.koodiUriExistsInKoodisto(ValintatapaKoodisto, "valintatapajono_av#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.koodiUriExistsInKoodisto(ValintatapaKoodisto, "valintatapajono_tv#1")).thenAnswer(itemFound)

    when(hakukohdeDao.listByValintaperusteId(valintaperusteId, TilaFilter.onlyOlemassaolevat()))
      .thenAnswer(Seq[HakukohdeListItem]())
  }

  "Valintaperuste validation" should "succeed when new valid valintaperuste" in {
    passesValidation(max)
  }

  it should "succeed when incomplete valintaperuste-luonnos" in {
    passesValidation(min)
  }

  it should "fail when invalid perustiedot" in {
    failsValidation(min.copy(id = Some(valintaperusteId)), "id", notMissingMsg(Some(valintaperusteId)))
    failsValidation(min.copy(nimi = Map(), kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(min.copy(nimi = Map(Fi -> "nimi")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(max.copy(nimi = Map(Fi -> "nimi", Sv -> "")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(min.copy(organisaatioOid = OrganisaatioOid("1.2.3")), "organisaatioOid", validationMsg("1.2.3"))
    failsValidation(min.copy(organisaatioOid = OrganisaatioOid("")), "organisaatioOid", validationMsg(""))
  }

  it should "fail if koulutustyyppi changed in modify operation" in {
    failsModifyValidation(
      maxWithId.copy(koulutustyyppi = Amm),
      max,
      Seq(
        ValidationError("koulutustyyppi", notModifiableMsg("koulutustyyppiä", "valintaperusteelle")),
        ValidationError("metadata.tyyppi", InvalidMetadataTyyppi)
      )
    )
  }

  it should "fail if oid not given in modify operation" in {
    failsModifyValidation(min, min, Seq(ValidationError("id", missingMsg)))
  }

  it should "fail when invalid hakutapaKoodiUri" in {
    failsValidation(
      max.copy(hakutapaKoodiUri = Some("hakutapa_99#99")),
      "hakutapaKoodiUri",
      invalidHakutapaKoodiUri("hakutapa_99#99")
    )
  }

  it should "fail when invalid kohdejoukkoKoodiUri" in {
    failsValidation(
      max.copy(kohdejoukkoKoodiUri = Some("haunkohdejoukko_99#99")),
      "kohdejoukkoKoodiUri",
      invalidHaunKohdejoukkoKoodiUri("haunkohdejoukko_99#99")
    )
  }

  it should "fail when invalid valintakokeet" in {
    failsValidation(
      max.copy(valintakokeet =
        List(
          Valintakoe1.copy(tyyppiKoodiUri = Some("puppu")),
          Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_99#99"))
        )
      ),
      Seq(
        ValidationError("valintakokeet[0].tyyppiKoodiUri", invalidValintakoeTyyppiKooriuri("puppu")),
        ValidationError("valintakokeet[1].tyyppiKoodiUri", invalidValintakoeTyyppiKooriuri("valintakokeentyyppi_99#99"))
      )
    )
  }

  it should "fail when data missing from julkaistu valintaperuste" in {
    failsValidation(
      max.copy(hakutapaKoodiUri = None, kohdejoukkoKoodiUri = None),
      Seq(
        ValidationError("hakutapaKoodiUri", missingMsg),
        ValidationError("kohdejoukkoKoodiUri", missingMsg)
      )
    )
  }

  "Valintaperuste validation in modify operation" should "succeed when hakutapaKoodiUri not changed, eventhough koodiUri is unknown" in {
    passesValidation(
      maxWithId.copy(hakutapaKoodiUri = Some("hakutapa_99#99")),
      max.copy(hakutapaKoodiUri = Some("hakutapa_99#99"))
    )
  }

  it should "succeed when kohdejoukkoKoodiUri not changed, eventhough koodiUri is unknown" in {
    passesValidation(
      maxWithId.copy(kohdejoukkoKoodiUri = Some("haunkohdejoukko_99#99")),
      max.copy(kohdejoukkoKoodiUri = Some("haunkohdejoukko_99#99"))
    )
  }

  it should "succeed when valintakokeet not changed, eventhogh unknown valintakoetyyppi" in {
    val valintakokeet = List(Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_9#1")))
    passesValidation(maxWithId.copy(valintakokeet = valintakokeet), max.copy(valintakokeet = valintakokeet))
  }

  it should "succeed when valintatavat not changed, eventhogh unknown valintatapaKoodiUri" in {
    val valintatapa = Valintatapa1.copy(valintatapaKoodiUri = Some("valintatapajono_XX#1"))
    passesValidation(
      maxWithValintatapa(valintatapa).copy(id = Some(UUID.randomUUID())),
      maxWithValintatapa(valintatapa)
    )
  }

  "Metadata validation" should "fail if invalid sisältö in valintaperuste" in {
    failsValidation(
      max.copy(metadata =
        Some(
          maxMetadata.copy(sisalto =
            Seq(Taulukko(id = None, rows = Seq(Row(-1, false, Seq(Column(-2, vainSuomeksi))))))
          )
        )
      ),
      Seq(
        ValidationError("metadata.sisalto[0].rows[0].index", notNegativeMsg),
        ValidationError("metadata.sisalto[0].rows[0].columns[0].index", notNegativeMsg),
        ValidationError("metadata.sisalto[0].rows[0].columns[0].text", kielistettyWoSvenskaError)
      )
    )
    failsValidation(
      max.copy(metadata =
        Some(maxMetadata.copy(sisalto = Seq(Taulukko(id = None, nimi = vainSuomeksi), SisaltoTeksti(vainSuomeksi))))
      ),
      Seq(
        ValidationError("metadata.sisalto[0].nimi", kielistettyWoSvenskaError),
        ValidationError("metadata.sisalto[1].teksti", kielistettyWoSvenskaError)
      )
    )
  }

  it should "fail when data missing from julkaistu valintaperuste" in {
    failsValidation(
      max.copy(metadata =
        Some(
          maxMetadata.copy(
            kuvaus = vainSuomeksi,
            hakukelpoisuus = vainSuomeksi,
            lisatiedot = vainSuomeksi,
            valintakokeidenYleiskuvaus = vainSuomeksi
          )
        )
      ),
      Seq(
        ValidationError("metadata.kuvaus", kielistettyWoSvenskaError),
        ValidationError("metadata.hakukelpoisuus", kielistettyWoSvenskaError),
        ValidationError("metadata.lisatiedot", kielistettyWoSvenskaError),
        ValidationError("metadata.valintakokeidenYleiskuvaus", kielistettyWoSvenskaError)
      )
    )
  }

  "Valintatapa validation" should "fail if invalid nimi" in {
    failsValidation(
      maxWithValintatapa(Valintatapa1.copy(nimi = vainSuomeksi)),
      "metadata.valintatavat[0].nimi",
      kielistettyWoSvenskaError
    )
  }

  it should "fail if invalid valintatapaKoodiUri" in {
    failsValidation(
      maxWithValintatapa(Valintatapa1.copy(valintatapaKoodiUri = Some("valintatapajono_XX#1"))),
      "metadata.valintatavat[0].valintatapaKoodiUri",
      invalidValintatapaKoodiUri("valintatapajono_XX#1")
    )
  }

  it should "fail if invalid sisältö" in {
    failsValidation(
      maxWithValintatapa(
        Valintatapa1.copy(sisalto = Seq(Taulukko(id = None, rows = Seq(Row(-1, false, Seq(Column(-2, vainSuomeksi)))))))
      ),
      Seq(
        ValidationError("metadata.valintatavat[0].sisalto[0].rows[0].index", notNegativeMsg),
        ValidationError("metadata.valintatavat[0].sisalto[0].rows[0].columns[0].index", notNegativeMsg),
        ValidationError("metadata.valintatavat[0].sisalto[0].rows[0].columns[0].text", kielistettyWoSvenskaError)
      )
    )
  }

  it should "fail if invalid pisteet" in {
    failsValidation(
      maxWithValintatapa(Valintatapa1.copy(vahimmaispisteet = Some(-1), enimmaispisteet = Some(-2))),
      Seq(
        ValidationError("metadata.valintatavat[0].enimmaispisteet", notNegativeMsg),
        ValidationError("metadata.valintatavat[0].vahimmaispisteet", notNegativeMsg),
        ValidationError("metadata.valintatavat[0].vahimmaispisteet", minmaxMsg(-1.0, -2.0))
      )
    )
  }

  it should "fail when data missing from julkaistu valintaperuste" in {
    failsValidation(
      maxWithValintatapa(
        Valintatapa1.copy(valintatapaKoodiUri = None, kynnysehto = vainSuomeksi)
      ),
      Seq(
        ValidationError("metadata.valintatavat[0].valintatapaKoodiUri", missingMsg),
        ValidationError("metadata.valintatavat[0].kynnysehto", kielistettyWoSvenskaError)
      )
    )
  }

  "Validate on julkaisu" should "fail when valintakoetilaisuus not in future" in {
    val paattyy = inPast(500)
    failsModifyValidation(maxWithKoeTilaisuudenAika(Ajanjakso(inPast(1000), Some(paattyy))).copy(id = Some(valintaperusteId)),
      maxWithId.copy(tila = Tallennettu), Seq(
        ValidationError("valintakokeet[0].tilaisuudet[0].aika.paattyy", pastDateMsg(paattyy))
      )
    )
  }

  it should "succeed when state changes from arkistoitu to julkaistu, eventhough valintakoetilaisuus not in future" in {
    passesValidation(
      maxWithKoeTilaisuudenAika(Ajanjakso(inPast(1000), Some(inPast(500)))).copy(id = Some(valintaperusteId)),
      maxWithId.copy(tila = Arkistoitu)
    )
  }

  "State change" should "succeed from tallennettu to julkaistu" in {
    passesValidation(maxWithId, maxWithId.copy(tila = Tallennettu))
  }

  it should "succeed from julkaistu to arkistoitu" in {
    passesValidation(maxWithId.copy(tila = Arkistoitu), maxWithId)
  }

  it should "succeed from arkistoitu to julkaistu" in {
    passesValidation(maxWithId, maxWithId.copy(tila = Arkistoitu))
  }

  it should "succeed from julkaistu to tallennettu" in {
    passesValidation(maxWithId.copy(tila = Tallennettu), maxWithId)
  }

  it should "succeed from tallennettu to poistettu" in {
    passesValidation(maxWithId.copy(tila = Poistettu), maxWithId.copy(tila = Tallennettu))
  }

  def failStageChangeValidation(newTila: Julkaisutila, oldTila: Julkaisutila): Assertion =
    failsStageChangeValidation(
      maxWithId.copy(tila = newTila),
      maxWithId.copy(tila = oldTila),
      illegalStateChange("valintaperusteelle", oldTila, newTila)
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
    val vp = max.copy(id = Some(valintaperusteId2))
    when(hakukohdeDao.listByValintaperusteId(valintaperusteId2, TilaFilter.onlyOlemassaolevat()))
      .thenAnswer(Seq(MinHakukohdeListItem))
    failsModifyValidation(
      vp.copy(tila = Poistettu),
      vp.copy(tila = Tallennettu),
      Seq(
        ValidationError("tila", integrityViolationMsg("Valintaperustetta", "hakukohteita"))
      )
    )
  }
}
