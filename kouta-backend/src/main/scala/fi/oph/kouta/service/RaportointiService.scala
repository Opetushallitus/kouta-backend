package fi.oph.kouta.service

import fi.oph.kouta.client.{OppijanumerorekisteriClient, SiirtotiedostoPalveluClient}
import fi.oph.kouta.domain.Hakukohde
import fi.oph.kouta.domain.oid.UserOid
import fi.oph.kouta.domain.raportointi._
import fi.oph.kouta.repository.RaportointiDAO
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.NameHelper

import java.time.LocalDateTime

object RaportointiService
    extends RaportointiService(KoulutusService, ToteutusService, HakukohdeService, OppijanumerorekisteriClient, SiirtotiedostoPalveluClient) {
  def apply(
      koulutusService: KoulutusService,
      toteutusService: ToteutusService,
      hakukohdeService: HakukohdeService,
      oppijanumerorekisteriClient: OppijanumerorekisteriClient,
      siirtotiedostoPalveluClient: SiirtotiedostoPalveluClient
  ): RaportointiService = {
    new RaportointiService(koulutusService, toteutusService, hakukohdeService, oppijanumerorekisteriClient, siirtotiedostoPalveluClient)
  }
}
class RaportointiService(
    koulutusService: KoulutusService,
    toteutusService: ToteutusService,
    hakukohdeService: HakukohdeService,
    oppijanumerorekisteriClient: OppijanumerorekisteriClient,
    siirtotiedostoPalveluClient: SiirtotiedostoPalveluClient
) {
  def saveKoulutukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime]) (implicit
      authenticated: Authenticated
  ): String = {
    val koulutukset           = RaportointiDAO.listKoulutukset(startTime, endTime).map(k => koulutusService.enrichKoulutus(k))
    val koulutusRaporttiItems = koulutukset.map(k => new KoulutusRaporttiItem(k))


    siirtotiedostoPalveluClient.saveSiirtotiedosto(startTime, endTime, "koulutukset", koulutusRaporttiItems)
  }
  def saveToteutukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val toteutukset           = RaportointiDAO.listToteutukset(startTime, endTime).map(t => toteutusService.enrichToteutus(t))
    val toteutusRaporttiItems = toteutukset.map(t => new ToteutusRaporttiItem(t))

    siirtotiedostoPalveluClient.saveSiirtotiedosto(startTime, endTime, "toteutukset", toteutusRaporttiItems)
  }

  def saveHakukohteet(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val hakukohteet = RaportointiDAO
      .listHakukohteet(startTime, endTime)
      .map(hri => {
        val hakuKohde = Hakukohde(
          toteutusOid = hri.toteutusOid,
          nimi = hri.nimi,
          hakuOid = hri.hakuOid,
          muokkaaja = hri.muokkaaja,
          organisaatioOid = hri.organisaatioOid
        )
        val enrichedData = hakukohdeService.enrichHakukohde(hakuKohde)._enrichedData
        hri.copy(_enrichedData =
          enrichedData.map(e => HakukohdeEnrichedDataRaporttiItem(e.esitysnimi, e.muokkaajanNimi))
        )
      })

    siirtotiedostoPalveluClient.saveSiirtotiedosto(startTime, endTime, "hakukohteet", hakukohteet)
  }

  private def getMuokkaajanNimi(userOid: UserOid): Option[String] = {
    val muokkaaja = oppijanumerorekisteriClient.getHenkilöFromCache(userOid)
    Some(NameHelper.generateMuokkaajanNimi(muokkaaja))
  }

  def saveHaut(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val haut = RaportointiDAO
      .listHaut(startTime, endTime)
    val hakuRaporttiItems = haut.map(h =>
      new HakuRaporttiItem(h).copy(_enrichedData = Some(HakuEnrichedDataRaporttiItem(getMuokkaajanNimi(h.muokkaaja))))
    )

    siirtotiedostoPalveluClient.saveSiirtotiedosto(startTime, endTime, "haut", hakuRaporttiItems)
  }

  def saveValintaperusteet(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val valintaPerusteet = RaportointiDAO
      .listValintaperusteet(startTime, endTime)
      .map(v => v.copy(_enrichedData = Some(ValintaperusteEnrichedDataRaporttiItem(getMuokkaajanNimi(v.muokkaaja)))))
    siirtotiedostoPalveluClient.saveSiirtotiedosto(startTime, endTime, "valintaperusteet", valintaPerusteet)
  }

  def saveSorakuvaukset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val sorakuvaukset = RaportointiDAO
      .listSorakuvaukset(startTime, endTime)
    val sorakuvausRaporttiItems = sorakuvaukset.map(s =>
      new SorakuvausRaporttiItem(s)
        .copy(_enrichedData = Some(SorakuvausEnrichedDataRaporttiItem(getMuokkaajanNimi(s.muokkaaja))))
    )
    siirtotiedostoPalveluClient.saveSiirtotiedosto(
      startTime,
      endTime,
      "sorakuvaukset",
      sorakuvausRaporttiItems
    )
  }

  def saveOppilaitokset(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val oppilaitokset = RaportointiDAO
      .listOppilaitokset(startTime, endTime)
    val oppilaitosRaporttiItems = oppilaitokset.map(s =>
      new OppilaitosRaporttiItem(s)
        .copy(_enrichedData = Some(OppilaitosEnrichedDataRaporttiItem(getMuokkaajanNimi(s.muokkaaja))))
    )
    siirtotiedostoPalveluClient.saveSiirtotiedosto(
      startTime,
      endTime,
      "oppilaitokset",oppilaitosRaporttiItems
    )
  }

  def saveOppilaitoksenOsat(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val oppilaitoksenOsat = RaportointiDAO
      .listOppilaitoksenOsat(startTime, endTime)
    val oppilaitoksenOsaRaporttiItems = oppilaitoksenOsat.map(s =>
      new OppilaitoksenOsaRaporttiItem(s)
        .copy(_enrichedData = Some(OppilaitosEnrichedDataRaporttiItem(getMuokkaajanNimi(s.muokkaaja))))
    )
    siirtotiedostoPalveluClient.saveSiirtotiedosto(
      startTime,
      endTime,
      "oppilaitoksenosat",oppilaitoksenOsaRaporttiItems
    )
  }

  def savePistehistoria(startTime: Option[LocalDateTime], endTime: Option[LocalDateTime])(implicit
      authenticated: Authenticated
  ): String = {
    val pisteHistoria = RaportointiDAO
      .listPistehistoria(startTime, endTime)
      .map(new PistetietoRaporttiItem(_))
    siirtotiedostoPalveluClient.saveSiirtotiedosto(startTime, endTime, "pistehistoria", pisteHistoria)
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
