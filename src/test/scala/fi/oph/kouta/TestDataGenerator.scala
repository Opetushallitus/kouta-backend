package fi.oph.kouta

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

import fi.oph.kouta.EmbeddedJettyLauncher.{DefaultPort, TestDataGeneratorSessionId}
import fi.oph.kouta.TestData._
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.fixture.{Id, Oid}
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.http.DefaultHttpRequest
import org.json4s.jackson.Serialization.{read, write}
import scalaj.http.Http

import scala.util.Random.shuffle

/* Generate random test data to local kouta-backend */
object TestDataGenerator extends KoutaJsonFormats {

  def inPast() = LocalDateTime.now().minusWeeks(2).truncatedTo(ChronoUnit.MINUTES)
  def now() = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
  def inFuture() = LocalDateTime.now().plusWeeks(2).truncatedTo(ChronoUnit.MINUTES)

  val KoutaBackendPath = System.getProperty(
    "test-data-generator.path",
    s"http://localhost:${System.getProperty("kouta-backend.port", DefaultPort)}/kouta-backend")

  val SorakuvausCount = 2
  val ValintaperusteCount = 2
  val HakuCount = 4

  val KoulutusCount = 5
  val ToteutusCount = 10

  val DebugOids = false

  val userOid = UserOid("1.2.246.562.24.62301161440")

  def main(args: Array[String]) {
    println(s"Generating test data to path $KoutaBackendPath")
    println(s"Koulutus count = $KoulutusCount")
    println(s"Starting...")

    val sorakuvausIds = (0 until SorakuvausCount).map(i => put("/sorakuvaus", sorakuvaus(i)))
    val valintaperusteIds = (0 until ValintaperusteCount).map(i => put("/valintaperuste", valintaperuste(i, sorakuvausIds(i))))
    println(s"Valintaperusteet ready...")

    val hakuOids = (0 until HakuCount).map(i => put("/haku", haku(i)))
    println(s"Haut ready...")

    println(s"Generating koulutukset, toteutukset ja hakukohteet. Wait for a moment...")
    (0 until KoulutusCount).foreach { i =>
      val koulutusOid = put("/koulutus", koulutus(i))
      (0 until ToteutusCount).foreach { j =>
        val toteutusOid = put("/toteutus", toteutus(koulutusOid, i, j))
        Seq(hakuOids.lift(i % 2), hakuOids.lift(i % 2 + 2)).flatten.foreach { hakuOid =>
          put("/hakukohde", hakukohde(i, j, 0, toteutusOid, hakuOid, valintaperusteIds(i % 2)))
        }
      }
    }

    println(s"Koulutukset, toteutukset ja hakukohteet ready...")

    println(s"Generating oppilaitokset...")
    val oppilaitosOid0 = ChildOid.s
    val oppilaitosOid1 = OtherOid.s

    putUnlessExists("/oppilaitos", oppilaitos(oppilaitosOid0), oppilaitosOid0)
    putUnlessExists("/oppilaitoksen-osa", oppilaitoksenOsa("1.2.246.562.10.76662434703", oppilaitosOid0), "1.2.246.562.10.76662434703")
    putUnlessExists("/oppilaitos", oppilaitos(oppilaitosOid1), oppilaitosOid1)
    putUnlessExists("/oppilaitoksen-osa", oppilaitoksenOsa("1.2.246.562.10.31388359585", oppilaitosOid1), "1.2.246.562.10.31388359585")
    putUnlessExists("/oppilaitoksen-osa", oppilaitoksenOsa("1.2.246.562.10.875704637010", oppilaitosOid1), "1.2.246.562.10.875704637010")

    println(s"DONE.")
    println(s"Test session id=$TestDataGeneratorSessionId")
  }

  def debug(oid: String, path: String) = {
    if(DebugOids) {
      println(s"$oid $path")
    }
    oid
  }

  private val organisaatioOids = Seq(OtherOid, ParentOid)

  def organisaatioOid(i: Int) = organisaatioOids(i % 2)

  def getTarjoajat(i: Int) = i % 4 match {
    case 0 => List(GrandChildOid, EvilGrandChildOid)
    case 2 => List(GrandChildOid, EvilCousin)
    case _ => List(LonelyOid, EvilGrandChildOid)
  }

  private def koulutusNimi(i: Int) = shuffle(List[Kielistetty](
    Map(Fi -> s"Koulutus $i", Sv -> s"Koulutus $i sv"),
    Map(Fi -> s"Humanistinen koulutus $i", Sv -> s"Humanistinen koulutus $i sv"),
    Map(Fi -> s"Psykologian perustutkinto $i", Sv -> s"Psykologian perustutkinto $i sv"),
    Map(Fi -> s"Autoalan perustutkinto $i", Sv -> s"Autoaland perustutkinto $i sv"),
    Map(Fi -> s"Lääketieteen koulutus $i", Sv -> s"Lääketieteen koulutus $i sv"))).head

  def koulutus(i: Int) = i % 2 match {
    case 0 => AmmKoulutus.copy(
      nimi = koulutusNimi(i),
      tila = shuffle(Julkaisutila.values()).head,
      julkinen = (i % 4 == 0),
      organisaatioOid = organisaatioOid(i),
      tarjoajat = List(organisaatioOid(i)),
      muokkaaja = userOid
    )
    case _ => YoKoulutus.copy(
      nimi = koulutusNimi(i),
      tila = shuffle(Julkaisutila.values()).head,
      julkinen = (i % 4 == 0),
      organisaatioOid = organisaatioOid(i),
      tarjoajat = List(organisaatioOid(i)),
      muokkaaja = userOid
    )
  }

  def updateAsiasanat(metadata: AmmatillinenToteutusMetadata): AmmatillinenToteutusMetadata = {
    metadata.copy(
      asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka"), Keyword(Fi, "musiikkioppilaitokset"),  Keyword(Fi, "hevoset"), Keyword(Fi, "psykologia")),
      ammattinimikkeet = List(Keyword(Fi, "automaatioinsinööri"), Keyword(Fi, "koneinsinööri"), Keyword(Fi, "muusikko"), Keyword(Fi, "psykologi"), Keyword(Fi, "hevonen"))
    )
  }

  def toteutus(koulutusOid: String, i: Int, j: Int) = i % 2 match {
    case 0 => JulkaistuAmmToteutus.copy(
      nimi = Map(Fi -> s"Koulutuksen $i toteutus $j", Sv -> s"Koulutuksen $i toteutus $j sv"),
      tila = shuffle(Julkaisutila.values()).head,
      koulutusOid = KoulutusOid(koulutusOid),
      organisaatioOid = organisaatioOid(i),
      tarjoajat = getTarjoajat(i),
      muokkaaja = userOid,
      metadata = Some(updateAsiasanat(JulkaistuAmmToteutus.metadata.get.asInstanceOf[AmmatillinenToteutusMetadata])))
    case 1 => JulkaistuYoToteutus.copy(
      nimi = Map(Fi -> s"Koulutuksen $i toteutus $j", Sv -> s"Koulutuksen $i toteutus $j sv"),
      tila = shuffle(Julkaisutila.values()).head,
      koulutusOid = KoulutusOid(koulutusOid),
      organisaatioOid = organisaatioOid(i),
      tarjoajat = getTarjoajat(i),
      muokkaaja = userOid)
  }

  def haku(i: Int) = JulkaistuHaku.copy(
    nimi = Map(Fi -> s"Haku $i", Sv -> s"Haku $i sv"),
    tila = shuffle(Julkaisutila.values()).head,
    hakuajat = List(Ajanjakso(alkaa = inPast(), paattyy = inFuture())),
    organisaatioOid = organisaatioOid(i)
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

  def sorakuvaus(i: Int) = AmmSorakuvaus.copy(
    nimi = Map(Fi -> s"Sorakuvaus $i", Sv -> s"Sorakuvaus $i sv"),
    tila = shuffle(Julkaisutila.values()).head,
    organisaatioOid = organisaatioOid(i)
  )

  def valintaperuste(i: Int, sorakuvausId: String) = i % 2 match {
    case 0 => AmmValintaperuste.copy(
      nimi = Map(Fi -> s"Valintaperuste $i", Sv -> s"Valintaperuste $i sv"),
      tila = shuffle(Julkaisutila.values()).head,
      sorakuvausId = Some(UUID.fromString(sorakuvausId)),
      organisaatioOid = organisaatioOid(i))
    case 1 => YoValintaperuste.copy(
      nimi = Map(Fi -> s"Valintaperuste $i", Sv -> s"Valintaperuste $i sv"),
      tila = shuffle(Julkaisutila.values()).head,
      sorakuvausId = Some(UUID.fromString(sorakuvausId)),
      organisaatioOid = organisaatioOid(i))
  }

  def oppilaitos(oid: String) = JulkaistuOppilaitos.copy(
    oid = OrganisaatioOid(oid),
    organisaatioOid = OrganisaatioOid(oid)
  )

  def oppilaitoksenOsa(oid: String, oppilaitosOid: String) = JulkaistuOppilaitoksenOsa.copy(
    oid = OrganisaatioOid(oid),
    oppilaitosOid = OrganisaatioOid(oppilaitosOid),
    organisaatioOid = OrganisaatioOid(oid)
  )

  def put[T <: AnyRef](path: String, data: T): String =
    new DefaultHttpRequest(
      Http(s"$KoutaBackendPath$path").method("PUT").header("Cookie", s"session=$TestDataGeneratorSessionId").put(write(data).getBytes)
    ).responseWithHeaders match {
      case (200, _, result) if path == "/valintaperuste" => debug(id(result).toString, path)
      case (200, _, result) if path == "/sorakuvaus" => debug(id(result).toString, path)
      case (200, _, result) => debug(oid(result), path)
      case (xxx, _, result) => throw new RuntimeException(
        s"Got status code $xxx from kouta-backend with response body [$result]! Cannot continue generating test data...")
    }

  def exists(path: String) = new DefaultHttpRequest(
    Http(s"$KoutaBackendPath$path").header("Cookie", s"session=$TestDataGeneratorSessionId")
  ).responseWithHeaders match {
    case (200, _, _) => true
    case (404, _, _) => false
    case (xxx, _, result) => throw new RuntimeException(
      s"Got status code $xxx from kouta-backend with response body [$result]! Cannot continue generating test data...")
  }

  def putUnlessExists[T <: AnyRef](path: String, data: T, id: String): Unit =
    if (!exists(s"$path/$id")) {
      put(path: String, data: T)
    }

  def oid(body: String) = (read[Oid](body)).oid

  def id(body: String) = (read[Id](body)).id
}
