package fi.oph.kouta.service

import fi.oph.kouta.client.{OppijanumerorekisteriClient, SiirtotiedostoPalveluClient}
import fi.oph.kouta.domain.{Hakukohde, LukiolinjaTieto, ToteutusEnrichmentSourceData}
import fi.oph.kouta.domain.oid.UserOid
import fi.oph.kouta.domain.raportointi._
import fi.oph.kouta.repository.RaportointiDAO
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.NameHelper

import java.time.LocalDateTime

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
  def saveKoulutukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val koulutusRaporttiItems = RaportointiDAO
      .listKoulutukset(startTime, endTime)
      .map(k =>
        k.copy(enrichedData =
          Some(new KoulutusEnrichedDataRaporttiItem(koulutusService.enrichKoulutus(k.ePerusteId, k.nimi, k.muokkaaja)))
        )
      )
    siirtotiedostoPalveluClient.saveSiirtotiedosto(startTime, endTime, "koulutukset", koulutusRaporttiItems)
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
    val toteutusRaporttiItems            = RaportointiDAO.listToteutukset(startTime, endTime)
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

    siirtotiedostoPalveluClient.saveSiirtotiedosto(startTime, endTime, "toteutukset", toteutusRaporttiItemsEnriched)
  }

  def saveHakukohteet(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val hakukohdeRaporttiItems = RaportointiDAO
      .listHakukohteet(startTime, endTime)
      .map(hri =>
        hri.copy(enrichedData =
          Some(
            new HakukohdeEnrichedDataRaporttiItem(
              hakukohdeService.enrichHakukohde(hri.muokkaaja, hri.nimi, hri.toteutusOid)
            )
          )
        )
      )
    siirtotiedostoPalveluClient.saveSiirtotiedosto(startTime, endTime, "hakukohteet", hakukohdeRaporttiItems)
  }

  private def getMuokkaajanNimi(userOid: UserOid): Option[String] = {
    val muokkaaja = oppijanumerorekisteriClient.getHenkilöFromCache(userOid)
    Some(NameHelper.generateMuokkaajanNimi(muokkaaja))
  }

  def saveHaut(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val hakuRaporttiItems = RaportointiDAO
      .listHaut(startTime, endTime)
      .map(h => h.copy(enrichedData = Some(HakuEnrichedDataRaporttiItem(getMuokkaajanNimi(h.muokkaaja)))))
    siirtotiedostoPalveluClient.saveSiirtotiedosto(startTime, endTime, "haut", hakuRaporttiItems)
  }

  def saveValintaperusteet(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val valintaPerusteRaporttiItems = RaportointiDAO
      .listValintaperusteet(startTime, endTime)
      .map(v => v.copy(enrichedData = Some(ValintaperusteEnrichedDataRaporttiItem(getMuokkaajanNimi(v.muokkaaja)))))
    siirtotiedostoPalveluClient.saveSiirtotiedosto(startTime, endTime, "valintaperusteet", valintaPerusteRaporttiItems)
  }

  def saveSorakuvaukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val sorakuvausRaporttiItems = RaportointiDAO
      .listSorakuvaukset(startTime, endTime)
      .map(s => s.copy(enrichedData = Some(SorakuvausEnrichedDataRaporttiItem(getMuokkaajanNimi(s.muokkaaja)))))
    siirtotiedostoPalveluClient.saveSiirtotiedosto(
      startTime,
      endTime,
      "sorakuvaukset",
      sorakuvausRaporttiItems
    )
  }

  def saveOppilaitoksetJaOsat(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val oppilaitosTaiOsaRaporttiItems = RaportointiDAO
      .listOppilaitoksetAndOsat(startTime, endTime)
      .map(o => o.copy(enrichedData = Some(OppilaitosOrOsaEnrichedDataRaporttiItem(getMuokkaajanNimi(o.muokkaaja)))))
    siirtotiedostoPalveluClient.saveSiirtotiedosto(
      startTime,
      endTime,
      "oppilaitoksetJaOsat",
      oppilaitosTaiOsaRaporttiItems
    )
  }

  def savePistehistoria(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val pistetietoRaporttiItems = RaportointiDAO
      .listPistehistoria(startTime, endTime)
    siirtotiedostoPalveluClient.saveSiirtotiedosto(startTime, endTime, "pistehistoria", pistetietoRaporttiItems)
  }

  def saveAmmattinimikkeet()(implicit
      authenticated: Authenticated
  ): String = {
    val ammattinimikkeet = RaportointiDAO
      .listAmmattinimikkeet()
    siirtotiedostoPalveluClient.saveSiirtotiedosto(None, None, "ammattinimikkeet", ammattinimikkeet)
  }

  def saveAsiasanat()(implicit
      authenticated: Authenticated
  ): String = {
    val asiasanat = RaportointiDAO
      .listAsiasanat()
    siirtotiedostoPalveluClient.saveSiirtotiedosto(None, None, "asiasanat", asiasanat)
  }
}
