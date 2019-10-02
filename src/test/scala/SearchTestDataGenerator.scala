package fi.oph.kouta

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

import fi.oph.kouta.EmbeddedJettyLauncher.{DefaultPort, TestDataGeneratorSessionId}
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

/* Generate julkaistu data for testing oppija searches */
object SearchTestDataGenerator extends KoutaJsonFormats {

  def inPast() = LocalDateTime.now().minusWeeks(2).truncatedTo(ChronoUnit.MINUTES)
  def now() = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
  def inFuture() = LocalDateTime.now().plusWeeks(2).truncatedTo(ChronoUnit.MINUTES)

  val KoutaBackendPath = System.getProperty(
    "test-data-generator.path",
    s"http://localhost:${System.getProperty("kouta-backend.port", DefaultPort)}/kouta-backend")

  val KoulutusCount = 10
  val DebugOids = false

  val userOid = "1.2.246.562.24.62301161440"

  val oppilaitosOid1 = "1.2.246.562.10.81934895871"
  val oppilaitosOid1Toimipiste1 = "1.2.246.562.10.94620157316" //Heinola
  val oppilaitosOid1Toimipiste2 = "1.2.246.562.10.43328767116" //Lahti
  val oppilaitosOid2 = "1.2.246.562.10.67476956288"
  val oppilaitosOid2Toimipiste1 = "1.2.246.562.10.59078453392" //Helsinki Myllypuro
  val oppilaitosOid2Toimipiste2 = "1.2.246.562.10.31775378982" //Helsinki Hämeentie
  val oppilaitosOid3 = "1.2.246.562.10.51720121923"
  val oppilaitosOid2Toimipiste1 = "1.2.246.562.10.42659724407" //Espoon keskus
  val oppilaitosOid2Toimipiste2 = "1.2.246.562.10.75537153407" //Leppävaara
  val oppilaitosOid4 = "1.2.246.562.10.52251087186"
  val oppilaitosOid2Toimipiste1 = "1.2.246.562.10.16538823663" //Helsinki sturenkatu
  val oppilaitosOid2Toimipiste2 = "1.2.246.562.10.45854578546" //Helsinki Myllypuro


  def main(args: Array[String]) {
    println(s"Generating search test data to path $KoutaBackendPath")
    println(s"Koulutus count = $KoulutusCount")
    println(s"Starting...")

    val sorakuvausId     = put("/sorakuvaus", sorakuvaus())
    val valintaperusteId = put("/valintaperuste", valintaperuste(sorakuvausId))

    put("/oppilaitos", oppilaitos(oppilaitosOid1))
    put("/oppilaitos", oppilaitos(oppilaitosOid2))
    put("/oppilaitos", oppilaitos(oppilaitosOid3))
    put("/oppilaitos", oppilaitos(oppilaitosOid4))





    println(s"Valintaperuste ready...")

    /*val hakuOid0 = put("/haku", haku(0))
    val hakuOid1 = put("/haku", haku(1))
    val hakuOid2 = put("/haku", haku(2))
    val hakuOid3 = put("/haku", haku(3))*/

    //println(s"Haut ready...")

    put("/koulutus", koulutus())


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

    println(s"Generating oppilaitoikset...")
    val oppilaitosOid0 = "1.2.246.562.10.81934895871"
    val oppilaitosOid1 = "1.2.246.562.10.67476956288"

    put("/oppilaitos", oppilaitos(oppilaitosOid0))
    put("/oppilaitoksen-osa", oppilaitoksenOsa("1.2.246.562.10.76662434703", oppilaitosOid0))
    put("/oppilaitos", oppilaitos(oppilaitosOid1))
    put("/oppilaitoksen-osa", oppilaitoksenOsa("1.2.246.562.10.31388359585", oppilaitosOid1))
    put("/oppilaitoksen-osa", oppilaitoksenOsa("1.2.246.562.10.875704637010", oppilaitosOid1))

    println(s"DONE.")
    println(s"Test session id=$TestDataGeneratorSessionId")
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
    case (_, 0) => List("1.2.246.562.10.76662434703", "1.2.246.562.10.31388359585").map(OrganisaatioOid)
    case (0, _) => List("1.2.246.562.10.76662434703", "1.2.246.562.10.875704637010").map(OrganisaatioOid)
    case (_, _) => List("1.2.246.562.10.31388359585", "1.2.246.562.10.875704637010").map(OrganisaatioOid)
  }

  def koulutus(nimi: String, tarjoajat: Seq[String], koulutusalaKoodiUrit: Seq[String]) = AmmKoulutus.copy(
    nimi = shuffle(List[Kielistetty](Map(Fi -> s"Koulutus $i", Sv -> s"Koulutus $i sv"),
      Map(Fi -> s"Humanistinen koulutus $i", Sv -> s"Humanistinen koulutus $i sv"),
      Map(Fi -> s"Psykologian perustutkinto $i", Sv -> s"Psykologian perustutkinto $i sv"),
      Map(Fi -> s"Autoalan perustutkinto $i", Sv -> s"Autoaland perustutkinto $i sv"),
      Map(Fi -> s"Lääketieteen koulutus $i", Sv -> s"Lääketieteen koulutus $i sv"))).head,
    tila = shuffle(Julkaisutila.values()).head,
    julkinen = (i%4 == 0),
    organisaatioOid = OrganisaatioOid(organisaatioOid(i)),
    tarjoajat = getTarjoajat(i),
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

  def sorakuvaus() = AmmSorakuvaus.copy(
    nimi = Map(Fi -> s"Sorakuvaus", Sv -> s"Sorakuvaus sv"),
    tila = Julkaistu,
    organisaatioOid = OrganisaatioOid(oppilaitosOid1)
  )

  def valintaperuste(sorakuvausId: String) = AmmValintaperuste.copy(
    nimi = Map(Fi -> s"Valintaperuste", Sv -> s"Valintaperuste sv"),
    tila = Julkaistu,
    sorakuvausId = Some(UUID.fromString(sorakuvausId)),
    organisaatioOid = OrganisaatioOid(oppilaitosOid1)
  )

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
      case (200, _, result) if path == "/valintaperuste" => debug(id(result).toString)
      case (200, _, result) if path == "/sorakuvaus" => debug(id(result).toString)
      case (200, _, result) => debug(oid(result))
      case (xxx, _, result) => throw new RuntimeException(
        s"Got status code $xxx from kouta-backend with response body [$result]! Cannot continue generating test data...")
    }

  def oid(body: String) = (read[Oid](body)).oid

  def id(body: String) = (read[Id](body)).id
}

