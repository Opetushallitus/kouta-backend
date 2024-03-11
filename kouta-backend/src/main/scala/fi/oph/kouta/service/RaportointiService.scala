package fi.oph.kouta.service

import fi.oph.kouta.client.{OppijanumerorekisteriClient, SiirtotiedostoPalveluClient}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.UserOid
import fi.oph.kouta.domain.raportointi._
import fi.oph.kouta.domain.{LukiolinjaTieto, ToteutusEnrichmentSourceData}
import fi.oph.kouta.repository.RaportointiDAO
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.NameHelper

import java.time.{LocalDateTime, ZonedDateTime}
import scala.collection.mutable.ListBuffer

object RaportointiService
    extends RaportointiService(
      KoulutusService,
      ToteutusService,
      HakukohdeService,
      OppijanumerorekisteriClient,
      SiirtotiedostoPalveluClient
    ) {
  def apply(
      koulutusService: KoulutusService,
      toteutusService: ToteutusService,
      hakukohdeService: HakukohdeService,
      oppijanumerorekisteriClient: OppijanumerorekisteriClient,
      siirtotiedostoPalveluClient: SiirtotiedostoPalveluClient
  ): RaportointiService = {
    new RaportointiService(
      koulutusService,
      toteutusService,
      hakukohdeService,
      oppijanumerorekisteriClient,
      siirtotiedostoPalveluClient
    )
  }
}
class RaportointiService(
    koulutusService: KoulutusService,
    toteutusService: ToteutusService,
    hakukohdeService: HakukohdeService,
    oppijanumerorekisteriClient: OppijanumerorekisteriClient,
    siirtotiedostoPalveluClient: SiirtotiedostoPalveluClient
) {
  val maxNumberOfItemsInFile = KoutaConfigurationFactory.configuration.s3Configuration.transferFileMaxItemCount;
  val NO_RESULTS_MESSAGE     = "Ei hakutuloksia annetuilla aikarajoilla"

  private def hakutulosMessage(totalSize: Int, entityDesc: String, keys: Seq[String]) = {
    val keyText = if (keys.size > 1) "avaimet" else "avain"
    s"Yhteensä $totalSize $entityDesc tallennettu S3 buckettiin, $keyText ${keys.mkString(", ")}"
  }

  private def getFileCreationTime: ZonedDateTime = ZonedDateTime.now(SiirtotiedostoTimezone)

  def saveKoulutukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    var totalSize: Int = 0
    var koulutusRaporttiItems =
      RaportointiDAO.listKoulutukset(startTime, endTime, maxNumberOfItemsInFile, totalSize)
    val keyList: ListBuffer[String] = ListBuffer()
    val fileCreationTime: ZonedDateTime = getFileCreationTime
    while (koulutusRaporttiItems.nonEmpty) {
      totalSize += koulutusRaporttiItems.size
      val enriched = koulutusRaporttiItems.map(k =>
        k.copy(enrichedData =
          Some(new KoulutusEnrichedDataRaporttiItem(koulutusService.enrichKoulutus(k.ePerusteId, k.nimi, k.muokkaaja)))
        )
      )
      keyList += siirtotiedostoPalveluClient.saveSiirtotiedosto(fileCreationTime, "koulutus", enriched)
      koulutusRaporttiItems = RaportointiDAO.listKoulutukset(startTime, endTime, maxNumberOfItemsInFile, totalSize)
    }
    if (totalSize > 0) hakutulosMessage(totalSize, "koulutusta", keyList) else NO_RESULTS_MESSAGE
  }

  private def lukioLinjat(lukioToteutusMetadata: LukioToteutusMetadataRaporttiItem) =
    (lukioToteutusMetadata.painotukset ++ lukioToteutusMetadata.erityisetKoulutustehtavat).map(l =>
      LukiolinjaTieto(l.koodiUri, l.kuvaus)
    )

  private def toteutusEnrichmentSourceData(t: ToteutusRaporttiItem, k: Option[KoulutusEnrichmentData]) = {
    val m = t.lukioToteutusMetadata()
    (k, m) match {
      case (Some(k), Some(m)) =>
        ToteutusEnrichmentSourceData(
          t.nimi,
          k.koulutuksetKoodiUri,
          t.muokkaaja.get,
          isLukioToteutus = true,
          lukioLinjat(m),
          m.yleislinja,
          k.opintojenLaajuusNumero()
        )
      case (Some(k), _) => ToteutusEnrichmentSourceData(t.nimi, k.koulutuksetKoodiUri, t.muokkaaja.get)
      case (_, Some(m)) =>
        ToteutusEnrichmentSourceData(
          t.nimi,
          Seq(),
          t.muokkaaja.get,
          isLukioToteutus = true,
          lukioLinjat(m),
          m.yleislinja,
          None
        )
      case (_, _) => ToteutusEnrichmentSourceData(t.nimi, Seq(), t.muokkaaja.get)
    }
  }

  def saveToteutukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val fileCreationTime: ZonedDateTime = getFileCreationTime
    var totalSize: Int              = 0
    val keyList: ListBuffer[String] = ListBuffer()
    var toteutusRaporttiItems       = RaportointiDAO.listToteutukset(startTime, endTime, maxNumberOfItemsInFile, totalSize)
    while (toteutusRaporttiItems.nonEmpty) {
      totalSize += toteutusRaporttiItems.size

      val lukioKoulutusOids                = toteutusRaporttiItems.filter(_.isLukioToteutus).map(_.koulutusOid)
      val lukioKoulutusEnrichmentDataItems = RaportointiDAO.listKoulutusEnrichmentDataItems(lukioKoulutusOids)
      val toteutusRaporttiItemsEnriched = toteutusRaporttiItems.map(t => {
        val koulutusEnrichmentData =
          if (t.isLukioToteutus)
            lukioKoulutusEnrichmentDataItems.find(_.oid.s == t.koulutusOid.s)
          else None
        t.copy(enrichedData =
          Some(
            new ToteutusEnrichedDataRaporttiItem(
              toteutusService.enrichToteutus(toteutusEnrichmentSourceData(t, koulutusEnrichmentData))
            )
          )
        )
      })
      keyList +=
        siirtotiedostoPalveluClient.saveSiirtotiedosto(fileCreationTime, "toteutus", toteutusRaporttiItemsEnriched)
      toteutusRaporttiItems = RaportointiDAO.listToteutukset(startTime, endTime, maxNumberOfItemsInFile, totalSize)
    }
    if (totalSize > 0) hakutulosMessage(totalSize, "toteutusta", keyList) else NO_RESULTS_MESSAGE
  }

  def saveHakukohteet(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    var totalSize: Int              = 0
    val keyList: ListBuffer[String] = ListBuffer()
    val fileCreationTime: ZonedDateTime = getFileCreationTime

    var hakukohdeRaporttiItems = RaportointiDAO
      .listHakukohteet(startTime, endTime, maxNumberOfItemsInFile, totalSize)
    while (hakukohdeRaporttiItems.nonEmpty) {
      totalSize += hakukohdeRaporttiItems.size
      val enriched = hakukohdeRaporttiItems
        .map(hri =>
          hri.copy(enrichedData =
            Some(
              new HakukohdeEnrichedDataRaporttiItem(
                hakukohdeService.enrichHakukohde(hri.muokkaaja, hri.nimi, hri.toteutusOid)
              )
            )
          )
        )
      keyList += siirtotiedostoPalveluClient.saveSiirtotiedosto(fileCreationTime, "hakukohde", enriched)
      hakukohdeRaporttiItems = RaportointiDAO
        .listHakukohteet(startTime, endTime, maxNumberOfItemsInFile, totalSize)
    }
    if (totalSize > 0) hakutulosMessage(totalSize, "hakukohdetta", keyList) else NO_RESULTS_MESSAGE
  }

  private def getMuokkaajanNimi(userOid: UserOid): Option[String] = {
    val muokkaaja = oppijanumerorekisteriClient.getHenkilöFromCache(userOid)
    Some(NameHelper.generateMuokkaajanNimi(muokkaaja))
  }

  def saveHaut(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    var totalSize: Int              = 0
    val keyList: ListBuffer[String] = ListBuffer()
    val fileCreationTime: ZonedDateTime = getFileCreationTime
    var hakuRaporttiItems = RaportointiDAO
      .listHaut(startTime, endTime, maxNumberOfItemsInFile, totalSize)
    while (hakuRaporttiItems.nonEmpty) {
      totalSize += hakuRaporttiItems.size
      val enriched = hakuRaporttiItems
        .map(h => h.copy(enrichedData = Some(HakuEnrichedDataRaporttiItem(getMuokkaajanNimi(h.muokkaaja)))))
      keyList += siirtotiedostoPalveluClient.saveSiirtotiedosto(fileCreationTime, "haku", enriched)
      hakuRaporttiItems = RaportointiDAO
        .listHaut(startTime, endTime, maxNumberOfItemsInFile, totalSize)
    }
    if (totalSize > 0) hakutulosMessage(totalSize, "hakua", keyList) else NO_RESULTS_MESSAGE
  }

  def saveValintaperusteet(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    var totalSize: Int              = 0
    val keyList: ListBuffer[String] = ListBuffer()
    val fileCreationTime: ZonedDateTime = getFileCreationTime

    var valintaPerusteRaporttiItems = RaportointiDAO
      .listValintaperusteet(startTime, endTime, maxNumberOfItemsInFile, totalSize)
    while (valintaPerusteRaporttiItems.nonEmpty) {
      totalSize += valintaPerusteRaporttiItems.size
      val enriched = valintaPerusteRaporttiItems
        .map(v => v.copy(enrichedData = Some(ValintaperusteEnrichedDataRaporttiItem(getMuokkaajanNimi(v.muokkaaja)))))
      keyList += siirtotiedostoPalveluClient.saveSiirtotiedosto(fileCreationTime, "valintaperuste", enriched)
      valintaPerusteRaporttiItems = RaportointiDAO
        .listValintaperusteet(startTime, endTime, maxNumberOfItemsInFile, totalSize)
    }
    if (totalSize > 0) hakutulosMessage(totalSize, "valintaperustetta", keyList) else NO_RESULTS_MESSAGE
  }

  def saveSorakuvaukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    var totalSize: Int              = 0
    val keyList: ListBuffer[String] = ListBuffer()
    val fileCreationTime: ZonedDateTime = getFileCreationTime
    var sorakuvausRaporttiItems = RaportointiDAO
      .listSorakuvaukset(startTime, endTime, maxNumberOfItemsInFile, totalSize)
    while (sorakuvausRaporttiItems.nonEmpty) {
      totalSize += sorakuvausRaporttiItems.size
      val enriched = sorakuvausRaporttiItems.map(s =>
        s.copy(enrichedData = Some(SorakuvausEnrichedDataRaporttiItem(getMuokkaajanNimi(s.muokkaaja))))
      )
      keyList += siirtotiedostoPalveluClient.saveSiirtotiedosto(
        fileCreationTime,
        "sorakuvaus",
        enriched
      )
      sorakuvausRaporttiItems = RaportointiDAO
        .listSorakuvaukset(startTime, endTime, maxNumberOfItemsInFile, totalSize)
    }
    if (totalSize > 0) hakutulosMessage(totalSize, "sorakuvausta", keyList) else NO_RESULTS_MESSAGE
  }

  def saveOppilaitoksetJaOsat(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    var olSize, osaSize: Int = 0
    val keyList: ListBuffer[String]            = ListBuffer()
    val fileCreationTime: ZonedDateTime = getFileCreationTime
    var queriedTypes                           = Seq(Oppilaitos, OppilaitoksenOsa)
    var oppilaitoksetAndOsat =
      queryOppilaitoksetAndOrOsatUpToMaxLimit(queriedTypes, startTime, endTime, olSize, osaSize)
    while (oppilaitoksetAndOsat._1.nonEmpty || oppilaitoksetAndOsat._2.nonEmpty) {
      olSize += oppilaitoksetAndOsat._1.size
      osaSize += oppilaitoksetAndOsat._2.size
      if (osaSize > 0) {
        // Jos kysely palauttaa oppilaitoksen osia > 0, kaikki oppilaitokset on siinä vaiheessa saatu haettua
        queriedTypes = Seq(OppilaitoksenOsa)
      }

      val enriched = (oppilaitoksetAndOsat._1 ++ oppilaitoksetAndOsat._2)
        .map(o => o.copy(enrichedData = Some(OppilaitosOrOsaEnrichedDataRaporttiItem(getMuokkaajanNimi(o.muokkaaja)))))
      keyList += siirtotiedostoPalveluClient.saveSiirtotiedosto(
        fileCreationTime,
        "oppilaitosJaOsa",
        enriched
      )
      oppilaitoksetAndOsat =
        queryOppilaitoksetAndOrOsatUpToMaxLimit(queriedTypes, startTime, endTime, olSize, osaSize)
    }
    if (olSize > 0 || osaSize > 0) {
      val keyText = if (keyList.size > 1) s"avaimet ${keyList.mkString(", ")}" else s"avain ${keyList.head}"
      s"""Yhteensä $olSize oppilaitosta, $osaSize oppilaitoksen osaa tallennettu S3 buckettiin, $keyText"""
    } else NO_RESULTS_MESSAGE
  }

  private def queryOppilaitoksetAndOrOsatUpToMaxLimit(
      queriedTypes: Seq[Organisaatiotyyppi],
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      oppilaitosOffset: Int,
      osaOffset: Int
  ): (Seq[OppilaitosOrOsaRaporttiItem], Seq[OppilaitosOrOsaRaporttiItem]) = {
    queriedTypes match {
      case Seq(_, _) =>
        val oppilaitokset =
          RaportointiDAO.listOppilaitokset(startTime, endTime, maxNumberOfItemsInFile, oppilaitosOffset)
        (
          oppilaitokset,
          if (oppilaitokset.size < maxNumberOfItemsInFile)
            RaportointiDAO.listOppilaitostenOsat(startTime, endTime, maxNumberOfItemsInFile - oppilaitokset.size, 0)
          else Seq()
        )
      case Seq(OppilaitoksenOsa) =>
        (Seq(), RaportointiDAO.listOppilaitostenOsat(startTime, endTime, maxNumberOfItemsInFile, osaOffset))
      case _ => (RaportointiDAO.listOppilaitokset(startTime, endTime, maxNumberOfItemsInFile, oppilaitosOffset), Seq())
    }
  }

  def savePistehistoria(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    var totalSize: Int              = 0
    val keyList: ListBuffer[String] = ListBuffer()
    val fileCreationTime: ZonedDateTime = getFileCreationTime
    var pistetietoRaporttiItems = RaportointiDAO
      .listPistehistoria(startTime, endTime, maxNumberOfItemsInFile, totalSize)
    while (pistetietoRaporttiItems.nonEmpty) {
      totalSize += pistetietoRaporttiItems.size
      keyList += siirtotiedostoPalveluClient.saveSiirtotiedosto(
        fileCreationTime,
        "pistehistoria",
        pistetietoRaporttiItems
      )
      pistetietoRaporttiItems = RaportointiDAO
        .listPistehistoria(startTime, endTime, maxNumberOfItemsInFile, totalSize)
    }
    if (totalSize > 0) hakutulosMessage(totalSize, "pistehistoriatietoa", keyList) else NO_RESULTS_MESSAGE
  }

  def saveAmmattinimikkeet()(implicit
      authenticated: Authenticated
  ): String = {
    var totalSize: Int              = 0
    val keyList: ListBuffer[String] = ListBuffer()
    val fileCreationTime: ZonedDateTime = getFileCreationTime

    var ammattinimikkeet = RaportointiDAO
      .listAmmattinimikkeet(maxNumberOfItemsInFile, totalSize)
    while (ammattinimikkeet.nonEmpty) {
      totalSize += ammattinimikkeet.size
      keyList += siirtotiedostoPalveluClient.saveSiirtotiedosto(fileCreationTime, "ammattinimike", ammattinimikkeet)
      ammattinimikkeet = RaportointiDAO
        .listAmmattinimikkeet(maxNumberOfItemsInFile, totalSize)
    }
    if (totalSize > 0) hakutulosMessage(totalSize, "ammattinimikettä", keyList) else "Ei hakutuloksia"
  }

  def saveAsiasanat()(implicit
      authenticated: Authenticated
  ): String = {
    var totalSize: Int              = 0
    val keyList: ListBuffer[String] = ListBuffer()
    val fileCreationTime: ZonedDateTime = getFileCreationTime

    var asiasanat = RaportointiDAO
      .listAsiasanat(maxNumberOfItemsInFile, totalSize)
    while (asiasanat.nonEmpty) {
      totalSize += asiasanat.size
      keyList += siirtotiedostoPalveluClient.saveSiirtotiedosto(fileCreationTime, "asiasana", asiasanat)
      asiasanat = RaportointiDAO
        .listAsiasanat(maxNumberOfItemsInFile, totalSize)
    }
    if (totalSize > 0) hakutulosMessage(totalSize, "asiasanaa", keyList) else "Ei hakutuloksia"
  }
}
