package fi.oph.kouta.service

import fi.oph.kouta.client.OppijanumerorekisteriClient

import java.time.Duration
import fi.oph.kouta.config.{KoutaConfigurationFactory, S3Configuration}
import fi.oph.kouta.domain.{HakuEnrichedData, Hakukohde}
import fi.oph.kouta.domain.oid.UserOid
import fi.oph.kouta.domain.raportointi.{HakuEnrichedDataRaporttiItem, HakuRaporttiItem, HakukohdeEnrichedDataRaporttiItem, KoulutusRaporttiItem, OppilaitoksenOsaRaporttiItem, OppilaitosEnrichedDataRaporttiItem, OppilaitosRaporttiItem, PistetietoRaporttiItem, SorakuvausEnrichedDataRaporttiItem, SorakuvausRaporttiItem, ToteutusRaporttiItem, ValintaperusteEnrichedDataRaporttiItem}
import fi.oph.kouta.repository.RaportointiDAO
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.{KoutaJsonFormats, NameHelper}
import fi.vm.sade.valinta.dokumenttipalvelu.SiirtotiedostoPalvelu
import org.json4s.jackson.Serialization.writePretty
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.presigner.S3Presigner

import java.io.{ByteArrayInputStream, InputStream}
import java.net.URI
import java.time.Instant
import java.util.{Date, Optional}

object RaportointiService
    extends RaportointiService(KoulutusService, ToteutusService, HakukohdeService, OppijanumerorekisteriClient) {
  def apply(
      koulutusService: KoulutusService,
      toteutusService: ToteutusService,
      hakukohdeService: HakukohdeService,
      oppijanumerorekisteriClient: OppijanumerorekisteriClient
  ): RaportointiService = {
    new RaportointiService(koulutusService, toteutusService, hakukohdeService, oppijanumerorekisteriClient)
  }
}
class RaportointiService(
    koulutusService: KoulutusService,
    toteutusService: ToteutusService,
    hakukohdeService: HakukohdeService,
    oppijanumerorekisteriClient: OppijanumerorekisteriClient
) extends KoutaJsonFormats {
  val config: S3Configuration = KoutaConfigurationFactory.configuration.s3Configuration;
  val siirtotiedostoPalvelu =
    new SiirtotiedostoPalvelu(config.region.getOrElse("eu-west-1"), config.transferFileBucket)

  private def saveEntitiesToS3(
      contentStartTime: Option[Instant],
      contentEndTime: Option[Instant],
      contentType: String,
      contentJson: String
  ) = {
    siirtotiedostoPalvelu.saveSiirtotiedosto(
      Optional.ofNullable(contentStartTime.map(Date.from(_)).orNull),
      Optional.ofNullable(contentEndTime.map(Date.from(_)).orNull),
      "kouta",
      Optional.of(contentType),
      new ByteArrayInputStream(contentJson.getBytes())
    )
  }
  def saveKoulutukset(startTime: Option[Instant], endTime: Option[Instant]) (implicit
      authenticated: Authenticated
  ): Unit = {
    val koulutukset           = RaportointiDAO.listKoulutukset(startTime, endTime).map(k => koulutusService.enrichKoulutus(k))
    val koulutusRaporttiItems = koulutukset.map(k => new KoulutusRaporttiItem(k))

    saveEntitiesToS3(startTime, endTime, "koulutukset", writePretty(koulutusRaporttiItems))
  }
  def saveToteutukset(startTime: Option[Instant], endTime: Option[Instant]) (implicit
      authenticated: Authenticated
  ): Unit = {
    val toteutukset           = RaportointiDAO.listToteutukset(startTime, endTime).map(t => toteutusService.enrichToteutus(t))
    val toteutusRaporttiItems = toteutukset.map(t => new ToteutusRaporttiItem(t))

    saveEntitiesToS3(startTime, endTime, "toteutukset", writePretty(toteutusRaporttiItems))
  }

  def saveHakukohteet(startTime: Option[Instant], endTime: Option[Instant]) (implicit
      authenticated: Authenticated
  ): Unit = {
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

    saveEntitiesToS3(startTime, endTime, "hakukohteet", writePretty(hakukohteet))
  }

  private def getMuokkaajanNimi(userOid: UserOid): Option[String] = {
    val muokkaaja = oppijanumerorekisteriClient.getHenkilöFromCache(userOid)
    Some(NameHelper.generateMuokkaajanNimi(muokkaaja))
  }

  def saveHaut(startTime: Option[Instant], endTime: Option[Instant]) (implicit
      authenticated: Authenticated
  ): Unit = {
    val haut = RaportointiDAO
      .listHaut(startTime, endTime)
    val hakuRaporttiItems = haut.map(h =>
      new HakuRaporttiItem(h).copy(_enrichedData = Some(HakuEnrichedDataRaporttiItem(getMuokkaajanNimi(h.muokkaaja))))
    )

    saveEntitiesToS3(startTime, endTime, "haut", writePretty(hakuRaporttiItems))
  }

  def saveValintaperusteet(startTime: Option[Instant], endTime: Option[Instant]) (implicit
      authenticated: Authenticated
  ): Unit = {
    val valintaPerusteet = RaportointiDAO
      .listValintaperusteet(startTime, endTime)
      .map(v => v.copy(_enrichedData = Some(ValintaperusteEnrichedDataRaporttiItem(getMuokkaajanNimi(v.muokkaaja)))))
    saveEntitiesToS3(startTime, endTime, "valintaperusteet", writePretty(valintaPerusteet))
  }

  def saveSorakuvaukset(startTime: Option[Instant], endTime: Option[Instant]) (implicit
      authenticated: Authenticated
  ): Unit = {
    val sorakuvaukset = RaportointiDAO
      .listSorakuvaukset(startTime, endTime)
    val sorakuvausRaporttiItems = sorakuvaukset.map(s =>
      new SorakuvausRaporttiItem(s)
        .copy(_enrichedData = Some(SorakuvausEnrichedDataRaporttiItem(getMuokkaajanNimi(s.muokkaaja))))
    )
    saveEntitiesToS3(startTime, endTime, "sorakuvaukset", writePretty(sorakuvausRaporttiItems))
  }

  def saveOppilaitokset(startTime: Option[Instant], endTime: Option[Instant]) (implicit
      authenticated: Authenticated
  ): Unit = {
    val oppilaitokset = RaportointiDAO
      .listOppilaitokset(startTime, endTime)
    val oppilaitosRaporttiItems = oppilaitokset.map(s =>
      new OppilaitosRaporttiItem(s)
        .copy(_enrichedData = Some(OppilaitosEnrichedDataRaporttiItem(getMuokkaajanNimi(s.muokkaaja))))
    )
    saveEntitiesToS3(startTime, endTime, "oppilaitokset", writePretty(oppilaitosRaporttiItems))
  }

  def saveOppilaitoksenOsat(startTime: Option[Instant], endTime: Option[Instant]) (implicit
      authenticated: Authenticated
  ): Unit = {
    val oppilaitoksenOsat = RaportointiDAO
      .listOppilaitoksenOsat(startTime, endTime)
    val oppilaitoksenOsaRaporttiItems = oppilaitoksenOsat.map(s =>
      new OppilaitoksenOsaRaporttiItem(s)
        .copy(_enrichedData = Some(OppilaitosEnrichedDataRaporttiItem(getMuokkaajanNimi(s.muokkaaja))))
    )
    saveEntitiesToS3(startTime, endTime, "oppilaitoksenosat", writePretty(oppilaitoksenOsaRaporttiItems))
  }

  def savePistehistoria(startTime: Option[Instant], endTime: Option[Instant]) (implicit
      authenticated: Authenticated
  ): Unit = {
    val pisteHistoria = RaportointiDAO
      .listPistehistoria(startTime, endTime).map(new PistetietoRaporttiItem(_))
    saveEntitiesToS3(startTime, endTime, "pistehistoria", writePretty(pisteHistoria))
  }

  def saveAmmattinimikkeet() (implicit
      authenticated: Authenticated
  ): Unit = {
    val ammattinimikkeet = RaportointiDAO
      .listAmmattinimikkeet()
    saveEntitiesToS3(None, None, "ammattinimikkeet", writePretty(ammattinimikkeet))
  }

  def saveAsiasanat() (implicit
      authenticated: Authenticated
  ): Unit = {
    val asiasanat = RaportointiDAO
      .listAsiasanat()
    saveEntitiesToS3(None, None, "asiasanat", writePretty(asiasanat))
  }
}
