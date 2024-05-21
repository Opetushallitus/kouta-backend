package fi.oph.kouta.service

import fi.oph.kouta.client.{OppijanumerorekisteriClient, SiirtotiedostoPalveluClient}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.UserOid
import fi.oph.kouta.domain.raportointi._
import fi.oph.kouta.domain.{LukiolinjaTieto, ToteutusEnrichmentSourceData, keyword}
import fi.oph.kouta.repository.RaportointiDAO
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.NameHelper

import java.time.LocalDateTime
import java.util.UUID
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

  def saveKoulutukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val result = saveEntitiesToSiirtotiedostot[KoulutusRaporttiItem](
      "koulutus",
      startTime,
      endTime,
      RaportointiDAO.listKoulutukset,
      enrichKoulutukset
    )
    if (result._1 > 0) hakutulosMessage(result._1, "koulutusta", result._2) else NO_RESULTS_MESSAGE
  }

  private def enrichKoulutukset(koulutusItems: Seq[KoulutusRaporttiItem]): Seq[KoulutusRaporttiItem] = {
    koulutusItems.map(k =>
      k.copy(enrichedData =
        Some(new KoulutusEnrichedDataRaporttiItem(koulutusService.enrichKoulutus(k.ePerusteId, k.nimi, k.muokkaaja)))
      )
    )
  }

  def saveToteutukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val result = saveEntitiesToSiirtotiedostot[ToteutusRaporttiItem](
      "toteutus",
      startTime,
      endTime,
      RaportointiDAO.listToteutukset,
      enrichToteutukset
    )
    if (result._1 > 0) hakutulosMessage(result._1, "toteutusta", result._2) else NO_RESULTS_MESSAGE
  }

  private def enrichToteutukset(toteutusItems: Seq[ToteutusRaporttiItem]): Seq[ToteutusRaporttiItem] = {
    val lukioKoulutusOids                = toteutusItems.filter(_.isLukioToteutus).map(_.koulutusOid)
    val lukioKoulutusEnrichmentDataItems = RaportointiDAO.listKoulutusEnrichmentDataItems(lukioKoulutusOids)
    toteutusItems.map(t => {
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

  def saveHakukohteet(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val result = saveEntitiesToSiirtotiedostot[HakukohdeRaporttiItem](
      "hakukohde",
      startTime,
      endTime,
      RaportointiDAO.listHakukohteet,
      enrichHakukohteet
    )
    if (result._1 > 0) hakutulosMessage(result._1, "hakukohdetta", result._2) else NO_RESULTS_MESSAGE
  }

  private def enrichHakukohteet(hakukohdeItems: Seq[HakukohdeRaporttiItem]): Seq[HakukohdeRaporttiItem] = {
    hakukohdeItems.map(hri =>
      hri.copy(enrichedData =
        Some(
          new HakukohdeEnrichedDataRaporttiItem(
            hakukohdeService.enrichHakukohde(hri.muokkaaja, hri.nimi, hri.toteutusOid)
          )
        )
      )
    )
  }

  private def getMuokkaajanNimi(userOid: UserOid): Option[String] = {
    val muokkaaja = oppijanumerorekisteriClient.getHenkilöFromCache(userOid)
    Some(NameHelper.generateMuokkaajanNimi(muokkaaja))
  }

  def saveHaut(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val result = saveEntitiesToSiirtotiedostot[HakuRaporttiItem](
      "haku",
      startTime,
      endTime,
      RaportointiDAO.listHaut,
      enrichHaut
    )
    if (result._1 > 0) hakutulosMessage(result._1, "hakua", result._2) else NO_RESULTS_MESSAGE
  }

  private def enrichHaut(hakuItems: Seq[HakuRaporttiItem]): Seq[HakuRaporttiItem] = {
    hakuItems.map(h => h.copy(enrichedData = Some(HakuEnrichedDataRaporttiItem(getMuokkaajanNimi(h.muokkaaja)))))
  }

  def saveValintaperusteet(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val result = saveEntitiesToSiirtotiedostot[ValintaperusteRaporttiItem](
      "valintaperuste",
      startTime,
      endTime,
      RaportointiDAO.listValintaperusteet,
      enrichValintaperusteet
    )
    if (result._1 > 0) hakutulosMessage(result._1, "valintaperustetta", result._2) else NO_RESULTS_MESSAGE
  }

  private def enrichValintaperusteet(valintaperusteItems: Seq[ValintaperusteRaporttiItem]): Seq[ValintaperusteRaporttiItem] = {
    valintaperusteItems
      .map(v => v.copy(enrichedData = Some(ValintaperusteEnrichedDataRaporttiItem(getMuokkaajanNimi(v.muokkaaja)))))
  }

  def saveSorakuvaukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val result = saveEntitiesToSiirtotiedostot[SorakuvausRaporttiItem](
      "sorakuvaus",
      startTime,
      endTime,
      RaportointiDAO.listSorakuvaukset,
      enrichSorakuvaukset
    )
    if (result._1 > 0) hakutulosMessage(result._1, "sorakuvausta", result._2) else NO_RESULTS_MESSAGE
  }

  private def enrichSorakuvaukset(sorakuvausItems: Seq[SorakuvausRaporttiItem]): Seq[SorakuvausRaporttiItem] = {
    sorakuvausItems
      .map(s =>
        s.copy(enrichedData = Some(SorakuvausEnrichedDataRaporttiItem(getMuokkaajanNimi(s.muokkaaja)))))
  }

  def saveOppilaitoksetJaOsat(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    var olSize, osaSize: Int        = 0
    val keyList: ListBuffer[String] = ListBuffer()
    var queriedTypes                = Seq(Oppilaitos, OppilaitoksenOsa)
    val operationId                 = UUID.randomUUID().toString
    var operationSubId              = 0;
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
      operationSubId += 1
      keyList += siirtotiedostoPalveluClient.saveSiirtotiedosto("oppilaitosJaOsa", enriched, operationId, operationSubId)
      oppilaitoksetAndOsat = queryOppilaitoksetAndOrOsatUpToMaxLimit(queriedTypes, startTime, endTime, olSize, osaSize)
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
    val result = saveEntitiesToSiirtotiedostot[PistetietoRaporttiItem](
      "pistehistoria",
      startTime,
      endTime,
      RaportointiDAO.listPistehistoria,
      {(pistetietoItems: Seq[PistetietoRaporttiItem]) => pistetietoItems }
    )
    if (result._1 > 0) hakutulosMessage(result._1, "pistehistoriatietoa", result._2) else NO_RESULTS_MESSAGE
  }

  def saveAmmattinimikkeet()(implicit
      authenticated: Authenticated
  ): String = {
    val result = saveEntitiesToSiirtotiedostot[keyword.Keyword](
      "ammattinimike",
      None,
      None,
      RaportointiDAO.listAmmattinimikkeet,
      {(ammattinimikkeet: Seq[keyword.Keyword]) => ammattinimikkeet }
    )
    if (result._1 > 0) hakutulosMessage(result._1, "ammattinimikettä", result._2) else NO_RESULTS_MESSAGE
  }

  def saveAsiasanat()(implicit
      authenticated: Authenticated
  ): String = {
    val result = saveEntitiesToSiirtotiedostot[keyword.Keyword](
      "asiasana",
      None,
      None,
      RaportointiDAO.listAsiasanat,
      {(asiasanat: Seq[keyword.Keyword]) => asiasanat }
    )
    if (result._1 > 0) hakutulosMessage(result._1, "asiasanaa", result._2) else NO_RESULTS_MESSAGE
  }

  private def saveEntitiesToSiirtotiedostot[T](
      entity: String,
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      queryFunc: (Option[LocalDateTime], Option[LocalDateTime], Int, Int) => Seq[T],
      enrichFunc: Seq[T] => Seq[T]
  ) = {
    var offsetCounter: Int          = 0;
    var raporttiItems               = queryFunc(startTime, endTime, maxNumberOfItemsInFile, offsetCounter)
    val keyList: ListBuffer[String] = ListBuffer()
    val operationId                 = UUID.randomUUID().toString
    var operationSubId              = 0;
    while (raporttiItems.nonEmpty) {
      offsetCounter += raporttiItems.size
      val enriched = enrichFunc(raporttiItems)
      operationSubId += 1
      keyList += siirtotiedostoPalveluClient.saveSiirtotiedosto(entity, enriched, operationId, operationSubId)
      raporttiItems = queryFunc(startTime, endTime, maxNumberOfItemsInFile, offsetCounter)
    }
    (offsetCounter, keyList)
  }
}
