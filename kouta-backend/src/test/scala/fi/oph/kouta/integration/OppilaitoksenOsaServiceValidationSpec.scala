package fi.oph.kouta.integration

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData.Osoite1
import fi.oph.kouta.TestOids.{ChildOid, EvilCousin, UnknownOid}
import fi.oph.kouta.client.HakuKoodiClient
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Fi, Julkaistu, NimettyLinkki, OppilaitoksenOsa, OppilaitoksenOsaMetadata, Oppilaitos, OppilaitosMetadata, Sv, Tallennettu, Yhteystieto}
import fi.oph.kouta.repository.OppilaitosDAO
import fi.oph.kouta.service.{OppilaitoksenOsaServiceValidation, OrganisaatioService}
import fi.oph.kouta.validation.ExternalQueryResults.itemFound
import fi.oph.kouta.validation.{BaseServiceValidationSpec, ValidationError}
import fi.oph.kouta.validation.Validations.{invalidPostiosoiteKoodiUri, invalidUrl, missingMsg, nonExistent, notNegativeMsg, notYetJulkaistu, validationMsg}

class OppilaitoksenOsaServiceValidationSpec extends BaseServiceValidationSpec[OppilaitoksenOsa] {
  val hakuKoodiClient      = mock[HakuKoodiClient]
  val oppilaitosDao       = mock[OppilaitosDAO]

  val min = TestData.MinOppilaitoksenOsa
  val max = TestData.JulkaistuOppilaitoksenOsa
  val maxMetadata = max.metadata.get
  val invalidOsoite = Some(Osoite1.copy(postinumeroKoodiUri = Some("puppu")))
  def minWithYhteystieto(yt: Yhteystieto): OppilaitoksenOsa =
    min.copy(metadata = Some(OppilaitoksenOsaMetadata(hakijapalveluidenYhteystiedot = Some(yt))))

  override val validator = new OppilaitoksenOsaServiceValidation(hakuKoodiClient, oppilaitosDao)

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(hakuKoodiClient.postiosoitekoodiExists("posti_04230#2")).thenAnswer(itemFound)
    when(hakuKoodiClient.postiosoitekoodiExists("posti_61100#2")).thenAnswer(itemFound)
    when(oppilaitosDao.getTila(ChildOid)).thenAnswer(Some(Julkaistu))
    when(oppilaitosDao.getTila(UnknownOid)).thenAnswer(None)
    when(oppilaitosDao.getTila(EvilCousin)).thenAnswer(Some(Tallennettu))
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
    passesValidation(osa, osa)
  }

  it should "fail if invalid data" in {
    failsValidation(minWithYhteystieto(Yhteystieto(kayntiosoite = invalidOsoite)), "metadata.hakijapalveluidenYhteystiedot.kayntiosoite.postinumeroKoodiUri", invalidPostiosoiteKoodiUri("puppu"))
  }
}
