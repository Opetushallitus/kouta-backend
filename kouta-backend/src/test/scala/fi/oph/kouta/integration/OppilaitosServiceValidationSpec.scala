package fi.oph.kouta.integration

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData.Osoite1
import fi.oph.kouta.client.HakuKoodiClient
import fi.oph.kouta.domain.{Fi, NimettyLinkki, Oppilaitos, OppilaitosMetadata, Sv, TietoaOpiskelusta, Yhteystieto}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.service.{OppilaitosServiceValidation, OrganisaatioService}
import fi.oph.kouta.validation.{BaseServiceValidationSpec, ValidationError}
import fi.oph.kouta.validation.ExternalQueryResults.itemFound
import fi.oph.kouta.validation.Validations.{
  invalidEmail,
  invalidKielistetty,
  invalidPostiosoiteKoodiUri,
  invalidTietoaOpiskelustaOtsikkoKoodiUri,
  invalidUrl,
  missingMsg,
  notNegativeMsg,
  validationMsg
}

class OppilaitosServiceValidationSpec extends BaseServiceValidationSpec[Oppilaitos] {
  val hakuKoodiClient     = mock[HakuKoodiClient]

  val min         = TestData.MinOppilaitos
  val max         = TestData.JulkaistuOppilaitos
  val maxMetadata = max.metadata.get

  val invalidOsoite = Some(Osoite1.copy(postinumeroKoodiUri = Some("puppu")))
  def minWithYhteystieto(yt: Yhteystieto): Oppilaitos =
    min.copy(metadata = Some(OppilaitosMetadata(hakijapalveluidenYhteystiedot = Some(yt))))

  override val validator = new OppilaitosServiceValidation(hakuKoodiClient)
  override def beforeEach(): Unit = {
    super.beforeEach()
    when(hakuKoodiClient.tietoaOpiskelustaOtsikkoKoodiUriExists("organisaationkuvaustiedot_03#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.postiosoitekoodiExists("posti_04230#2")).thenAnswer(itemFound)
    when(hakuKoodiClient.postiosoitekoodiExists("posti_61100#2")).thenAnswer(itemFound)
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
    passesValidation(oppilaitos, oppilaitos)
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
    passesValidation(oppilaitos, oppilaitos)
  }

  it should "succeed when kayntiosoite not changed, eventhough unknown postinumeroKoodiUri" in {
    val oppilaitos = minWithYhteystieto(Yhteystieto(kayntiosoite = invalidOsoite))
    passesValidation(oppilaitos, oppilaitos)
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
}
