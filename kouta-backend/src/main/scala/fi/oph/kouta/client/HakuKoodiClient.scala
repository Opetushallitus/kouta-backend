package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.client.HakukoodiConstants.{hakukohdeKoodistoAmmErityisopetus, hakukohdeKoodistoPoJalkYhteishaku}
import fi.oph.kouta.client.KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.PainotetutArvoSanatLukioKaikki
import fi.oph.kouta.validation.ExternalQueryResults.{ExternalQueryResult, fromBoolean, itemFound, queryFailed}
import fi.vm.sade.properties.OphProperties

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

object HakuKoodiClient extends HakuKoodiClient(KoutaConfigurationFactory.configuration.urlProperties)

package object HakukoodiConstants {
  val hakukohdeKoodistoAmmErityisopetus = "hakukohteeterammatillinenerityisopetus"
  val hakukohdeKoodistoPoJalkYhteishaku = "hakukohteetperusopetuksenjalkeinenyhteishaku"
}

class HakuKoodiClient(urlProperties: OphProperties) extends KoodistoClient(urlProperties) {
  implicit val koodiUriCache: Cache[String, Seq[KoodiUri]] = Scaffeine()
    .expireAfterWrite(15.minutes)
    .build()
  implicit val koodiuriVersionCache: Cache[String, KoodiUri] = Scaffeine()
    .expireAfterWrite(15.minutes)
    .build()

  def getKoodiUriVersionOrLatestFromCache(koodiUriAsString: String): Either[Throwable, KoodiUri] = {
    Try[KoodiUri] {
      koodiuriVersionCache.get(koodiUriAsString, koodiUriAsString => getKoodiUriVersionOrLatest(koodiUriAsString))
    } match {
      case Success(koodiUri) => Right(koodiUri)
      case Failure(exp) => Left(exp)
    }
  }

  def hakukohdeKoodiUriAmmErityisopetusExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto(hakukohdeKoodistoAmmErityisopetus, koodiUri)

  def hakukohdeKoodiUriPoJalkYhteishakuExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto(hakukohdeKoodistoPoJalkYhteishaku, koodiUri)

  def pohjakoulutusVaatimusKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("pohjakoulutusvaatimuskouta", koodiUri)

  def liiteTyyppiKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("liitetyypitamm", koodiUri)

  def valintakoeTyyppiKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("valintakokeentyyppi", koodiUri)

  def kausiKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("kausi", koodiUri)

  def oppiaineKoodiUriExists(koodiUri: String): ExternalQueryResult = {
    if (PainotetutArvoSanatLukioKaikki.koodiUrit contains koodiUri) {
      itemFound
    } else{
      koodiUriExistsInKoodisto("painotettavatoppiaineetlukiossa", koodiUri)
    }
  }
  def kieliKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("kieli", koodiUri)

  def postiosoitekoodiExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("posti", koodiUri)

  def hakutapaKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("hakutapa", koodiUri)

  def haunkohdejoukkoKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("haunkohdejoukko", koodiUri)

  def haunkohdejoukonTarkenneKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("haunkohdejoukontarkenne", koodiUri)

  def valintatapaKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("valintatapajono", koodiUri)

  def tietoaOpiskelustaOtsikkoKoodiUriExists(koodiUri: String): ExternalQueryResult =
    koodiUriExistsInKoodisto("organisaationkuvaustiedot", koodiUri)

  private def koodiUriExistsInKoodisto(koodisto: String, koodiUri: String): ExternalQueryResult = {
    getAndUpdateFromKoodiUriCache(koodisto, koodiUriCache) match {
      case resp if resp.success =>
        fromBoolean(koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, resp.koodiUritInKoodisto))
      case _ => queryFailed
    }
  }
}
