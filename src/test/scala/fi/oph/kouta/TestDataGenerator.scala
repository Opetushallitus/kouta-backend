package fi.oph.kouta

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

import fi.oph.kouta.EmbeddedJettyLauncher.DEFAULT_PORT
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.fixture.{Id, Oid}
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.http.DefaultHttpRequest
import org.json4s.jackson.Serialization.{read, write}
import scalaj.http.Http

import scala.util.Random.shuffle
import fi.oph.kouta.TestData._
import fi.oph.kouta.domain.keyword.Keyword

/* Generate random test data to local kouta-backend */
object TestDataGenerator extends KoutaJsonFormats {

  def inPast() = LocalDateTime.now().minusWeeks(2).truncatedTo(ChronoUnit.MINUTES)
  def now() = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
  def inFuture() = LocalDateTime.now().plusWeeks(2).truncatedTo(ChronoUnit.MINUTES)

  val KoutaBackendPath = System.getProperty(
    "test-data-generator.path",
    s"http://localhost:${System.getProperty("kouta-backend.port", DEFAULT_PORT)}/kouta-backend")

  val KoulutusCount = 500
  val DebugOids = false

  val userOid = "1.2.246.562.24.62301161440"

  def main(args: Array[String]) {
    println(s"Generating test data to path $KoutaBackendPath")
    println(s"Koulutus count = $KoulutusCount")
    println(s"Starting...")

    val valintaperusteId0 = put("/valintaperuste", valintaperuste(0))
    val valintaperusteId1 = put("/valintaperuste", valintaperuste(1))
    
    println(s"Valintaperusteet ready...")

    val hakuOid0 = put("/haku", haku(0))
    val hakuOid1 = put("/haku", haku(1))
    val hakuOid2 = put("/haku", haku(2))
    val hakuOid3 = put("/haku", haku(3))
    
    println(s"Haut ready...")

    println(s"Generating koulutukset, toteutukset ja hakukohteet. Wait for a momemnt...")
    (0 to KoulutusCount).foreach { i =>
      val koulutusOid = put("/koulutus", koulutus(i))
      (0 to 10).foreach { j =>
        val toteutusOid = put("/toteutus", toteutus(koulutusOid, i, j))
        if(i%2 == 0) {
          put("/hakukohde", hakukohde(i, j, 0, toteutusOid, hakuOid0, valintaperusteId0))
          put("/hakukohde", hakukohde(i, j, 1, toteutusOid, hakuOid2, valintaperusteId0))
        } else {
          put("/hakukohde", hakukohde(i, j, 0, toteutusOid, hakuOid1, valintaperusteId1))
          put("/hakukohde", hakukohde(i, j, 1, toteutusOid, hakuOid3, valintaperusteId1))
        }
      }
    }
    
    println(s"Koulutukset, toteutukset ja hakukohteet ready...")
    println(s"DONE.")
  }

  def debug(oid: String) = {
    if(DebugOids) {
      println(s"$oid")
    }
    oid
  }

  def organisaatioOid(i: Int) = (i%2) match {
    case 0 => "1.2.246.562.10.67476956288"
    case _ => "1.2.246.562.10.594252633210"
  }

  def getTarjoajat(i: Int) = ((i%2), (i%4)) match {
    case (_, 0) => List("1.2.246.562.10.23178783348", "1.2.246.562.10.69157007167").map(OrganisaatioOid)
    case (0, _) => List("1.2.246.562.10.23178783348", "1.2.246.562.10.875704637010").map(OrganisaatioOid)
    case (_, _) => List("1.2.246.562.10.94169710632", "1.2.246.562.10.69157007167").map(OrganisaatioOid)
  }

  def koulutus(i: Int) = AmmKoulutus.copy(
    nimi = shuffle(List[Kielistetty](Map(Fi -> s"Koulutus $i", Sv -> s"Koulutus $i sv"),
                                     Map(Fi -> s"Humanistinen koulutus $i", Sv -> s"Humanistinen koulutus $i sv"),
                                     Map(Fi -> s"Psykologian perustutkinto $i", Sv -> s"Psykologian perustutkinto $i sv"),
                                     Map(Fi -> s"Autoalan perustutkinto $i", Sv -> s"Autoaland perustutkinto $i sv"),
                                     Map(Fi -> s"Lääketieteen koulutus $i", Sv -> s"Lääketieteen koulutus $i sv"))).head,
    tila = shuffle(Julkaisutila.values()).head,
    julkinen = (i%4 == 0),
    organisaatioOid = OrganisaatioOid(organisaatioOid(i)),
    tarjoajat = List(OrganisaatioOid(organisaatioOid(i))),
    muokkaaja = UserOid(userOid)
  )

  def updateAsiasanat(metadata: AmmatillinenToteutusMetadata): AmmatillinenToteutusMetadata = {
    metadata.copy(
      asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka"), Keyword(Fi, "musiikkioppilaitokset"),  Keyword(Fi, "hevoset"), Keyword(Fi, "psykologia")),
      ammattinimikkeet = List(Keyword(Fi, "automaatioinsinööri"), Keyword(Fi, "koneinsinööri"), Keyword(Fi, "muusikko"), Keyword(Fi, "psykologi"), Keyword(Fi, "hevonen"))
    )
  }

  def toteutus(koulutusOid: String, i: Int, j: Int) = JulkaistuAmmToteutus.copy(
    nimi = Map(Fi -> s"Koulutuksen $i toteutus $j", Sv -> s"Koulutuksen $i toteutus $j sv"),
    tila = shuffle(Julkaisutila.values()).head,
    koulutusOid = KoulutusOid(koulutusOid),
    organisaatioOid = OrganisaatioOid(organisaatioOid(i)),
    tarjoajat = getTarjoajat(i),
    muokkaaja = UserOid(userOid),
    metadata = Some(updateAsiasanat(JulkaistuAmmToteutus.metadata.get.asInstanceOf[AmmatillinenToteutusMetadata]))
  )

  def haku(i: Int) = JulkaistuHaku.copy(
    nimi = Map(Fi -> s"Haku $i", Sv -> s"Haku $i sv"),
    tila = shuffle(Julkaisutila.values()).head,
    hakuajat = List(Ajanjakso(alkaa = inPast(), paattyy = inFuture())),
    organisaatioOid = OrganisaatioOid(organisaatioOid(i))
  )

  def hakukohde(i: Int, j: Int, k: Int, toteutusOid: String, hakuOid: String, valintaperusteId: String) = JulkaistuHakukohde.copy(
    nimi = Map(Fi -> s"Koulutuksen $i toteutuksen $j hakukohde $k", Sv -> s"Koulutuksen $i toteutuksen $j hakukohde $k sv"),
    tila = shuffle(Julkaisutila.values()).head,
    toteutusOid = ToteutusOid(toteutusOid),
    hakuOid = HakuOid(hakuOid),
    valintaperusteId = Some(UUID.fromString(valintaperusteId)),
    hakuajat = shuffle(List(List(Ajanjakso(alkaa = inPast(), paattyy = now())), List(Ajanjakso(alkaa = inPast(), paattyy = inFuture())))).head,
    kaytetaanHaunAikataulua = shuffle(List(Some(false), Some(true))).head,
    organisaatioOid = getTarjoajat(i)(k)
  )

  def valintaperuste(i: Int) = YoValintaperuste.copy(
    nimi = Map(Fi -> s"Valintaperuste $i", Sv -> s"Valintaperuste $i sv"),
    tila = shuffle(Julkaisutila.values()).head,
    organisaatioOid = OrganisaatioOid(organisaatioOid(i))
  )

  def put[T <: AnyRef](path: String, data: T): String =
    new DefaultHttpRequest(
      Http(s"$KoutaBackendPath$path").method("PUT").put(write(data).getBytes)
    ).responseWithHeaders match {
      case (200, _, result) if path == "/valintaperuste" => debug(id(result).toString)
      case (200, _, result) => debug(oid(result))
      case (xxx, _, result) => throw new RuntimeException(
        s"Got status code $xxx from kouta-backend with response body [$result]! Cannot continue generating test data...")
    }

  def oid(body: String) = (read[Oid](body)).oid

  def id(body: String) = (read[Id](body)).id
}
