package fi.oph.kouta.service

import fi.oph.kouta.client.{OppijanumerorekisteriClient, SiirtotiedostoPalveluClient}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.UserOid
import fi.oph.kouta.domain.siirtotiedosto._
import fi.oph.kouta.domain.{LukiolinjaTieto, TilaFilter, ToteutusEnrichmentSourceData, keyword}
import fi.oph.kouta.repository.{SiirtotiedostoDAO, SiirtotiedostoRaportointiDAO}
import fi.oph.kouta.util.NameHelper

import java.time.LocalDateTime
import java.util.UUID
import scala.collection.mutable.ListBuffer

object SiirtotiedostoService
    extends SiirtotiedostoService(
      SiirtotiedostoDAO,
      KoulutusService,
      ToteutusService,
      OppijanumerorekisteriClient,
      SiirtotiedostoPalveluClient,
      HakukohdeUtil
    )

object SiirtotiedostoRaportointiService
    extends SiirtotiedostoService(
      SiirtotiedostoRaportointiDAO,
      KoulutusService,
      ToteutusService,
      OppijanumerorekisteriClient,
      SiirtotiedostoPalveluClient,
      HakukohdeUtil
    )

class SiirtotiedostoService(
    siirtotiedostoDAO: SiirtotiedostoDAO,
    koulutusService: KoulutusService,
    toteutusService: ToteutusService,
    oppijanumerorekisteriClient: OppijanumerorekisteriClient,
    siirtotiedostoPalveluClient: SiirtotiedostoPalveluClient,
    hakukohdeUtil: HakukohdeUtil
) {
  val maxNumberOfItemsInFile = KoutaConfigurationFactory.configuration.s3Configuration.transferFileMaxItemCount;

  def saveKoulutukset(
      operationId: UUID,
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime]
  ): SiirtotiedostoOperationResults = {
    saveEntitiesToSiirtotiedostot[KoulutusRaporttiItem](
      "koulutus",
      startTime,
      endTime,
      operationId,
      siirtotiedostoDAO.listKoulutukset,
      enrichKoulutukset
    )
  }

  private def enrichKoulutukset(koulutusItems: Seq[KoulutusRaporttiItem]): Seq[KoulutusRaporttiItem] = {
    koulutusItems.map(k =>
      k.copy(enrichedData =
        Some(new KoulutusEnrichedDataRaporttiItem(koulutusService.enrichKoulutus(k.ePerusteId, k.nimi, k.muokkaaja)))
      )
    )
  }

  def saveToteutukset(
      operationId: UUID,
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime]
  ): SiirtotiedostoOperationResults = {
    saveEntitiesToSiirtotiedostot[ToteutusRaporttiItem](
      "toteutus",
      startTime,
      endTime,
      operationId,
      siirtotiedostoDAO.listToteutukset,
      enrichToteutukset
    )
  }

  private def enrichToteutukset(toteutusItems: Seq[ToteutusRaporttiItem]): Seq[ToteutusRaporttiItem] = {
    val lukioKoulutusOids                = toteutusItems.filter(_.isLukioToteutus).map(_.koulutusOid)
    val lukioKoulutusEnrichmentDataItems = siirtotiedostoDAO.listKoulutusEnrichmentDataItems(lukioKoulutusOids)
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

  def saveHakukohteet(
      operationId: UUID,
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime]
  ): SiirtotiedostoOperationResults = {
    saveEntitiesToSiirtotiedostot[HakukohdeRaporttiItem](
      "hakukohde",
      startTime,
      endTime,
      operationId,
      siirtotiedostoDAO.listHakukohteet,
      enrichHakukohteet
    )
  }

  private def enrichHakukohteet(hakukohdeItems: Seq[HakukohdeRaporttiItem]): Seq[HakukohdeRaporttiItem] = {
    hakukohdeItems.map(hri => {
      val toteutusItem = siirtotiedostoDAO.getSingleTotetutus(hri.toteutusOid, TilaFilter.onlyOlemassaolevat())
      hri.copy(enrichedData =
        Some(
          new HakukohdeEnrichedDataRaporttiItem(
            hakukohdeUtil.enrichHakukohde(hri.muokkaaja, hri.nimi, toteutusItem, hri.hakukohdeKoodiUri)
          )
        )
      )
    })
  }

  private def getMuokkaajanNimi(userOid: UserOid): Option[String] = {
    val muokkaaja = oppijanumerorekisteriClient.getHenkilöFromCache(userOid)
    Some(NameHelper.generateMuokkaajanNimi(muokkaaja))
  }

  def saveHaut(
      operationId: UUID,
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime]
  ): SiirtotiedostoOperationResults = {
    saveEntitiesToSiirtotiedostot[HakuRaporttiItem](
      "haku",
      startTime,
      endTime,
      operationId,
      siirtotiedostoDAO.listHaut,
      enrichHaut
    )
  }

  private def enrichHaut(hakuItems: Seq[HakuRaporttiItem]): Seq[HakuRaporttiItem] = {
    hakuItems.map(h => h.copy(enrichedData = Some(HakuEnrichedDataRaporttiItem(getMuokkaajanNimi(h.muokkaaja)))))
  }

  def saveValintaperusteet(
      operationId: UUID,
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime]
  ): SiirtotiedostoOperationResults = {
    saveEntitiesToSiirtotiedostot[ValintaperusteRaporttiItem](
      "valintaperuste",
      startTime,
      endTime,
      operationId,
      siirtotiedostoDAO.listValintaperusteet,
      enrichValintaperusteet
    )
  }

  private def enrichValintaperusteet(
      valintaperusteItems: Seq[ValintaperusteRaporttiItem]
  ): Seq[ValintaperusteRaporttiItem] = {
    valintaperusteItems
      .map(v => v.copy(enrichedData = Some(ValintaperusteEnrichedDataRaporttiItem(getMuokkaajanNimi(v.muokkaaja)))))
  }

  def saveSorakuvaukset(
      operationId: UUID,
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime]
  ): SiirtotiedostoOperationResults = {
    saveEntitiesToSiirtotiedostot[SorakuvausRaporttiItem](
      "sorakuvaus",
      startTime,
      endTime,
      operationId,
      siirtotiedostoDAO.listSorakuvaukset,
      enrichSorakuvaukset
    )
  }

  private def enrichSorakuvaukset(sorakuvausItems: Seq[SorakuvausRaporttiItem]): Seq[SorakuvausRaporttiItem] = {
    sorakuvausItems
      .map(s => s.copy(enrichedData = Some(SorakuvausEnrichedDataRaporttiItem(getMuokkaajanNimi(s.muokkaaja)))))
  }

  def saveOppilaitoksetJaOsat(
      operationId: UUID,
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime]
  ): SiirtotiedostoOperationResults = {
    var olSize, osaSize: Int        = 0
    val keyList: ListBuffer[String] = ListBuffer()
    var queriedTypes                = Seq(Oppilaitos, OppilaitoksenOsa)
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
      keyList += siirtotiedostoPalveluClient.saveSiirtotiedosto(
        "oppilaitoksetjaosat",
        enriched,
        operationId,
        operationSubId
      )
      oppilaitoksetAndOsat = queryOppilaitoksetAndOrOsatUpToMaxLimit(queriedTypes, startTime, endTime, olSize, osaSize)
    }
    SiirtotiedostoOperationResults(keyList.toSeq, olSize + osaSize, true)
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
          siirtotiedostoDAO.listOppilaitokset(startTime, endTime, maxNumberOfItemsInFile, oppilaitosOffset)
        (
          oppilaitokset,
          if (oppilaitokset.size < maxNumberOfItemsInFile)
            siirtotiedostoDAO.listOppilaitostenOsat(startTime, endTime, maxNumberOfItemsInFile - oppilaitokset.size, 0)
          else Seq()
        )
      case Seq(OppilaitoksenOsa) =>
        (Seq(), siirtotiedostoDAO.listOppilaitostenOsat(startTime, endTime, maxNumberOfItemsInFile, osaOffset))
      case _ =>
        (siirtotiedostoDAO.listOppilaitokset(startTime, endTime, maxNumberOfItemsInFile, oppilaitosOffset), Seq())
    }
  }

  def savePistehistoria(
      operationId: UUID,
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime]
  ): SiirtotiedostoOperationResults = {
    saveEntitiesToSiirtotiedostot[PistetietoRaporttiItem](
      "pistehistoria",
      startTime,
      endTime,
      operationId,
      siirtotiedostoDAO.listPistehistoria,
      { (pistetietoItems: Seq[PistetietoRaporttiItem]) => pistetietoItems }
    )
  }

  def saveAmmattinimikkeet(operationId: UUID): SiirtotiedostoOperationResults = {
    saveEntitiesToSiirtotiedostot[keyword.Keyword](
      "ammattinimike",
      None,
      None,
      operationId,
      siirtotiedostoDAO.listAmmattinimikkeet,
      { (ammattinimikkeet: Seq[keyword.Keyword]) => ammattinimikkeet }
    )
  }

  def saveAsiasanat(operationId: UUID): SiirtotiedostoOperationResults = {
    saveEntitiesToSiirtotiedostot[keyword.Keyword](
      "asiasana",
      None,
      None,
      operationId,
      siirtotiedostoDAO.listAsiasanat,
      { (asiasanat: Seq[keyword.Keyword]) => asiasanat }
    )
  }

  private def saveEntitiesToSiirtotiedostot[T](
      entity: String,
      startTime: Option[LocalDateTime],
      endTime: Option[LocalDateTime],
      operationId: UUID,
      queryFunc: (Option[LocalDateTime], Option[LocalDateTime], Int, Int) => Seq[T],
      enrichFunc: Seq[T] => Seq[T]
  ): SiirtotiedostoOperationResults = {
    var offsetCounter: Int          = 0;
    var raporttiItems               = queryFunc(startTime, endTime, maxNumberOfItemsInFile, offsetCounter)
    val keyList: ListBuffer[String] = ListBuffer()
    var operationSubId              = 0;
    while (raporttiItems.nonEmpty) {
      offsetCounter += raporttiItems.size
      val enriched = enrichFunc(raporttiItems)
      operationSubId += 1
      keyList += siirtotiedostoPalveluClient.saveSiirtotiedosto(entity, enriched, operationId, operationSubId)
      raporttiItems = queryFunc(startTime, endTime, maxNumberOfItemsInFile, offsetCounter)
    }
    SiirtotiedostoOperationResults(keyList.toSeq, offsetCounter, true)
  }

  def findLatestSiirtotiedostoData(): Option[Siirtotiedosto] = siirtotiedostoDAO.findLatestSiirtotiedostoData()
  def saveSiirtotiedostoData(siirtoTiedosto: Siirtotiedosto): Int =
    siirtotiedostoDAO.saveSiirtotiedostoData(siirtoTiedosto)
}
