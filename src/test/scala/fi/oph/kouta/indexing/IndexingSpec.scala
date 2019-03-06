package fi.oph.kouta.indexing

import java.time.LocalDateTime
import java.util.UUID

import org.scalatest.{OptionValues, WordSpec}
import org.scalatest.Matchers._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.indexing.Indexing._


class IndexingSpec extends WordSpec with OptionValues {
  case class Foo(id: Option[String])
  implicit val fooIndexing: Indexing[Foo] = new Indexing[Foo] {
    val index: String = "fooIndex"
    def indexId(a: Foo): Option[String] = a.id
  }

  val haku = Haku(
    oid = Some(HakuOid("haku.oid")),
    modified = Some(LocalDateTime.now),
    muokkaaja = UserOid("user.oid"),
    organisaatioOid = OrganisaatioOid("organisaatio.oid")
  )

  val hakukohde = Hakukohde(
    oid = Some(HakukohdeOid("hakukohde.oid")),
    toteutusOid = ToteutusOid("toteutus.oid"),
    hakuOid = HakuOid("hakukohde.oid"),
    modified = Some(LocalDateTime.now),
    muokkaaja = UserOid("user.oid"),
    organisaatioOid = OrganisaatioOid("organisaatio.oid")
  )

  val koulutus = Koulutus(
    oid = Some(KoulutusOid("koulutus.oid")),
    johtaaTutkintoon = true,
    modified = Some(LocalDateTime.now),
    muokkaaja = UserOid("user.oid"),
    organisaatioOid = OrganisaatioOid("organisaatio.oid")
  )

  val toteutus = Toteutus(
    oid = Some(ToteutusOid("toteutus.oid")),
    koulutusOid = KoulutusOid("koulutus.oid"),
    modified = Some(LocalDateTime.now),
    muokkaaja = UserOid("user.oid"),
    organisaatioOid = OrganisaatioOid("organisaatio.oid")
  )

  val valintaperuste = Valintaperuste(
    id = Some(UUID.randomUUID()),
    modified = Some(LocalDateTime.now),
    muokkaaja = UserOid("user.oid"),
    organisaatioOid = OrganisaatioOid("organisaatio.oid")
  )

  "Indexing.ops.indexMessage" should {
    "return indexing message" when {
      "given test class with implementation" in {
        ops.indexMessage(Foo(Some("foobar"))).value should be ("""{"fooIndex":["foobar"]}""")
      }

      "given Haku" in {
        ops.indexMessage(haku).value should be ("""{"haut":["haku.oid"]}""")
      }
      "given Hakukohde" in {
        ops.indexMessage(hakukohde).value should be ("""{"hakukohteet":["hakukohde.oid"]}""")
      }
      "given Koulutus" in {
        ops.indexMessage(koulutus).value should be ("""{"koulutukset":["koulutus.oid"]}""")
      }
      "given toteutus" in {
        ops.indexMessage(toteutus).value should be ("""{"toteutukset":["toteutus.oid"]}""")
      }
      "given Valintaperuste" in {
        ops.indexMessage(valintaperuste).value should be (s"""{"valintaperusteet":["${valintaperuste.id.get.toString}"]}""")
      }
    }
    "return None if oid/id is None" when {
      "given test class with implementation" in {
        ops.indexMessage(Foo(None)) should be (None)
      }

      "given Haku" in {
        ops.indexMessage(haku.copy(oid = None)) should be (None)
      }
      "given Hakukohde" in {
        ops.indexMessage(hakukohde.copy(oid = None)) should be (None)
      }
      "given Koulutus" in {
        ops.indexMessage(koulutus.copy(oid = None)) should be (None)
      }
      "given toteutus" in {
        ops.indexMessage(toteutus.copy(oid = None)) should be (None)
      }
      "given Valintaperuste" in {
        ops.indexMessage(valintaperuste.copy(id = None)) should be (None)
      }
    }
  }
}
