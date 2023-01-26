package fi.oph.kouta.integration

import fi.oph.kouta.TestData.{MinSorakuvaus, YoSorakuvaus}
import fi.oph.kouta.client.CachedKoodistoClient
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Amm, Arkistoitu, Fi, Julkaistu, Julkaisutila, KoulutusKoodisto, KoulutusalaKoodisto, Poistettu, Sorakuvaus, Sv, Tallennettu, TilaFilter}
import fi.oph.kouta.repository.KoulutusDAO
import fi.oph.kouta.service.SorakuvausServiceValidation
import fi.oph.kouta.validation.{BaseServiceValidationSpec, ValidationError}
import fi.oph.kouta.validation.ExternalQueryResults.itemFound
import fi.oph.kouta.validation.Validations.{illegalStateChange, integrityViolationMsg, invalidKielistetty, invalidKoulutusAlaKoodiuri, invalidKoulutuskoodiuri, missingMsg, notMissingMsg, notModifiableMsg, validationMsg}
import org.scalatest.Assertion

import java.util.UUID

class SorakuvausServiceValidationSpec extends BaseServiceValidationSpec[Sorakuvaus] {
  val koodistoClient = mock[CachedKoodistoClient]
  val koulutusDao = mock[KoulutusDAO]

  val sorakuvausId = UUID.randomUUID()
  val sorakuvausId2 = UUID.randomUUID()

  val max: Sorakuvaus = YoSorakuvaus
  val maxWithId = max.copy(id = Some(sorakuvausId))
  val min: Sorakuvaus = MinSorakuvaus
  val maxMetadata = max.metadata.get

  override val validator = new SorakuvausServiceValidation(koodistoClient, koulutusDao)

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(koodistoClient.koodiUriExistsInKoodisto(KoulutusKoodisto, "koulutus_371101#1")).thenAnswer(itemFound)
    when(koodistoClient.koodiUriExistsInKoodisto(KoulutusalaKoodisto, "kansallinenkoulutusluokitus2016koulutusalataso2_054#1"))
      .thenAnswer(itemFound)

    when(koulutusDao.listBySorakuvausId(sorakuvausId, TilaFilter.onlyOlemassaolevat())).thenAnswer(Seq[String]())
  }

  "Sorakuvaus validation" should "pass valid julkaistu sorakuvaus" in {
    passesValidation(max)
  }

  it should "pass incomplete sorakuvaus if not julkaistu" in {
    passesValidation(min)
  }

  it should "fail if perustiedot is invalid" in {
    failsValidation(min.copy(id = Some(sorakuvausId)), "id", notMissingMsg(Some(sorakuvausId)))
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
        ValidationError("koulutustyyppi", notModifiableMsg("koulutustyyppiä", "sorakuvaukselle"))
      )
    )
  }

  it should "fail if oid not given in modify operation" in {
    failsModifyValidation(min, min, Seq(ValidationError("id", missingMsg)))
  }

  it should "fail if metadata missing from julkaistu sorakuvaus" in {
    failsValidation(max.copy(metadata = None), "metadata", missingMsg)
  }

  "Metadata validation" should "fail if unknown koulutusKoodiUrit" in {
    failsValidation(
      max.copy(metadata = Some(maxMetadata.copy(koulutusKoodiUrit = Seq("koulutus_123456#1", "koulutus_654321#1")))),
      Seq(
        ValidationError("metadata.koulutusKoodiUrit[0]", invalidKoulutuskoodiuri("koulutus_123456#1")),
        ValidationError("metadata.koulutusKoodiUrit[1]", invalidKoulutuskoodiuri("koulutus_654321#1"))
      )
    )
  }

  it should "fail if unknown koulutusalaKoodiUri" in {
    failsValidation(
      max.copy(metadata = Some(maxMetadata.copy(koulutusalaKoodiUri = Some("puppu")))),
      "metadata.koulutusalaKoodiUri",
      invalidKoulutusAlaKoodiuri("puppu")
    )
  }

  it should "fail if metadata missing from julkaistu sorakuvaus" in {
    failsValidation(
      max.copy(metadata = Some(maxMetadata.copy(kuvaus = Map()))),
      "metadata.kuvaus",
      invalidKielistetty(Seq(Fi, Sv))
    )
  }

  "Sorakuvaus validation in modify operation" should "succeed when koulutusKoodiUrit not changed, eventhough koodiUrit unknown" in {
    val sk = maxWithId.copy(metadata =
      Some(maxMetadata.copy(koulutusKoodiUrit = Seq("koulutus_123456#1", "koulutus_654321#1")))
    )
    passesValidation(sk, sk)
  }

  it should "succeed when koulutusalaKoodiUri not changed, eventhough koodiUri unknown" in {
    val sk = maxWithId.copy(metadata = Some(maxMetadata.copy(koulutusalaKoodiUri = Some("puppu"))))
    passesValidation(sk, sk)
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
      illegalStateChange("sorakuvaukselle", oldTila, newTila)
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

  it should "fail from tallennettu to poistettu when existing koulutukset" in {
    val sk = max.copy(id = Some(sorakuvausId2))
    when(koulutusDao.listBySorakuvausId(sorakuvausId2, TilaFilter.onlyOlemassaolevat())).thenAnswer(Seq[String]("1.2.246.562.13.00000000000000000123"))
    failsModifyValidation(
      sk.copy(tila = Poistettu),
      sk.copy(tila = Tallennettu),
      Seq(
        ValidationError("tila", integrityViolationMsg("Sorakuvausta", "koulutuksia"))
      )
    )

  }
}