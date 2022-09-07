package fi.oph.kouta.external

import fi.oph.kouta.domain.{Hakutieto, MuuHakulomake, Poistettu}
import fi.oph.kouta.external.KoutaFixtureTool.{DefaultHaku, KaytetaanHaunHakulomakettaKey}

import java.util.UUID
import fi.oph.kouta.external.{KoutaFixtureTool => KFT}
import fi.oph.kouta.integration.{AccessControlSpec, EverythingFixture, KoutaIntegrationSpec}
import org.json4s.jackson.Serialization.read
import org.scalatest.BeforeAndAfterEach

import scala.collection.convert.ImplicitConversions.`map AsScala`

class KoutaFixtureToolSpec extends KoutaIntegrationSpec with EverythingFixture with BeforeAndAfterEach with AccessControlSpec {

  def ophHeaders: Seq[(String, String)] = Seq(sessionHeader(ophSession), jsonHeader)

  def doPut(path: String, entityJson: String, headers: Seq[(String, String)] = defaultHeaders): String =
    put(path, entityJson, headers) {
      withClue(body) {
        status should equal(200)
      }
      body
    }

  override def afterEach(): Unit = {
    super.afterEach()
    KFT.reset()
  }

  "Kouta fixture tool" should "be able to save default koulutus" in {
    val sorakuvausId = saveSorakuvaus

    val oid = "1.2.246.562.13.00000000000000000009"
    KFT.addKoulutus(oid, KFT.DefaultKoulutusScala + (KFT.SorakuvausIdKey -> sorakuvausId))
    val koulutus = KFT.getKoulutus(oid)
    doPut(KoulutusPath, koulutus, ophHeaders)
  }

  private def saveSorakuvaus = {
    val tempSorakuvausId = UUID.randomUUID().toString
    KFT.addSorakuvaus(tempSorakuvausId, KFT.DefaultSorakuvausScala)
    val sorakuvaus = KFT.getSorakuvaus(tempSorakuvausId)
    val sorakuvausId = id(doPut(SorakuvausPath, sorakuvaus, ophHeaders)).toString
    sorakuvausId
  }

  it should "be able to save default toteutus" in {
    val sorakuvausId = saveSorakuvaus

    KFT.addKoulutus("1.2.246.562.13.00000000000000000009", KFT.DefaultKoulutusScala + (KFT.SorakuvausIdKey -> sorakuvausId))
    val koulutus = KFT.getKoulutus("1.2.246.562.13.00000000000000000009")
    val koulutusOid = oid(doPut(KoulutusPath, koulutus, ophHeaders))

    val toteutusOid = "1.2.246.562.17.00000000000000000009"
    val toteutusMap = KFT.DefaultToteutusScala + (KFT.KoulutusOidKey -> koulutusOid)
    KFT.addToteutus(toteutusOid, toteutusMap)
    val toteutus = KFT.getToteutus(toteutusOid)
    doPut(ToteutusPath, toteutus)
  }

  it should "be able to save default haku" in {
    val oid = "1.2.246.562.29.00000000000000000009"
    KFT.addHaku(oid, KFT.DefaultHaku)
    val haku = KFT.getHaku(oid)
    doPut(HakuPath, haku)
  }

  it should "be able to save default sorakuvaus" in {
    val id = UUID.randomUUID().toString
    KFT.addSorakuvaus(id, KFT.DefaultSorakuvaus)
    val sorakuvaus = KFT.getSorakuvaus(id)
    doPut(SorakuvausPath, sorakuvaus, ophHeaders)
  }

  it should "be able to save default valintaperuste" in {
    val valintaperusteId = UUID.randomUUID().toString
    KFT.addValintaperuste(valintaperusteId, KFT.DefaultValintaperusteScala)
    val valintaperuste = KFT.getValintaperuste(valintaperusteId)
    doPut(ValintaperustePath, valintaperuste)
  }

  it should "be able to save default hakukohde" in {
    val sorakuvausId = saveSorakuvaus

    KFT.addKoulutus("1.2.246.562.13.00000000000000000009", KFT.DefaultKoulutusScala + (KFT.SorakuvausIdKey -> sorakuvausId))
    val koulutus = KFT.getKoulutus("1.2.246.562.13.00000000000000000009")
    val koulutusOid = oid(doPut(KoulutusPath, koulutus, ophHeaders))

    val toteutusMap = KFT.DefaultToteutusScala + (KFT.KoulutusOidKey -> koulutusOid)
    KFT.addToteutus("1.2.246.562.17.00000000000000000009", toteutusMap)
    val toteutus = KFT.getToteutus("1.2.246.562.17.00000000000000000009")
    val toteutusOid = oid(doPut(ToteutusPath, toteutus))

    KFT.addHaku("1.2.246.562.29.00000000000000000009", DefaultHaku)
    val haku = KFT.getHaku("1.2.246.562.29.00000000000000000009")
    val hakuOid = oid(doPut(HakuPath, haku))

    val tempValintaperusteId = UUID.randomUUID().toString
    KFT.addValintaperuste(tempValintaperusteId, KFT.DefaultValintaperusteScala)
    val valintaperuste = KFT.getValintaperuste(tempValintaperusteId)
    val valintaperusteId = id(doPut(ValintaperustePath, valintaperuste)).toString

    val hakukohdeOid = "1.2.246.562.20.00000000000000000009"
    val hakukohdeMap = KFT.DefaultHakukohdeScala ++
      Map(KFT.ToteutusOidKey -> toteutusOid, KFT.HakuOidKey -> hakuOid, KFT.ValintaperusteIdKey -> valintaperusteId)
    KFT.addHakukohde(hakukohdeOid, hakukohdeMap)
    val hakukohde = KFT.getHakukohde(hakukohdeOid)
    doPut(HakukohdePath, hakukohde)
  }

  it should "be able to save default oppilaitos" in {
    val oid = "1.2.246.562.10.00101010101"
    KFT.addOppilaitos(oid, KFT.DefaultOppilaitos)
    val oppilaitos = KFT.getOppilaitos(oid)
    doPut(OppilaitosPath, oppilaitos)
  }

  it should "be able to save default oppilaitoksen osa" in {
    val oppilaitosOid = "1.2.246.562.10.00101010102"
    KFT.addOppilaitos(oppilaitosOid, KFT.DefaultOppilaitos)
    val oppilaitos = KFT.getOppilaitos(oppilaitosOid)
    doPut(OppilaitosPath, oppilaitos)

    val oppilaitoksenOsaOid = "1.2.246.562.10.90101010101"
    val oppilaitosMap = KFT.DefaultOppilaitoksenOsaScala + (KFT.OppilaitosOidKey -> oppilaitosOid)
    KFT.addOppilaitoksenOsa(oppilaitoksenOsaOid, oppilaitosMap)
    val oppilaitoksenOsa = KFT.getOppilaitoksenOsa(oppilaitoksenOsaOid)
    doPut(OppilaitoksenOsaPath, oppilaitoksenOsa)
  }


  it should "be able to get hakutieto" in {
    val sorakuvausId = UUID.randomUUID().toString
    KFT.addSorakuvaus(sorakuvausId, KFT.DefaultSorakuvausScala)
    KFT.addKoulutus("1.2.246.562.13.00000000000000000009", KFT.DefaultKoulutusScala + (KFT.SorakuvausIdKey -> sorakuvausId))
    val toteutusMap = KFT.DefaultToteutusScala + (KFT.KoulutusOidKey -> "1.2.246.562.13.00000000000000000009")
    KFT.addToteutus("1.2.246.562.17.00000000000000000009", toteutusMap)
    KFT.addHaku("1.2.246.562.29.00000000000000000008", KFT.DefaultHakuScala)
    KFT.addHaku("1.2.246.562.29.00000000000000000009", KFT.DefaultHakuScala)
    val valintaperusteId = UUID.randomUUID().toString
    KFT.addValintaperuste(valintaperusteId, KFT.DefaultValintaperusteScala)
    val hakukohdeMap = KFT.DefaultHakukohdeScala ++
      Map(KFT.ToteutusOidKey -> "1.2.246.562.17.00000000000000000009", KFT.HakuOidKey -> "1.2.246.562.29.00000000000000000008", KFT.ValintaperusteIdKey -> valintaperusteId)
    KFT.addHakukohde("1.2.246.562.20.00000000000000000008", hakukohdeMap)
    val hakukohdeMap2 = KFT.DefaultHakukohdeScala ++
      Map(KFT.ToteutusOidKey -> "1.2.246.562.17.00000000000000000009", KFT.HakuOidKey -> "1.2.246.562.29.00000000000000000009", KFT.ValintaperusteIdKey -> valintaperusteId)
    KFT.addHakukohde("1.2.246.562.20.00000000000000000009", hakukohdeMap2)

    val hakutieto = read[List[Hakutieto]](KFT.getHakutiedotByKoulutus("1.2.246.562.13.00000000000000000009")).toArray

    hakutieto.size should equal (1)
    hakutieto(0).toteutusOid.toString() should equal ("1.2.246.562.17.00000000000000000009")
    val haut = hakutieto(0).haut.toArray
    haut.size should equal (2)
    haut(0).hakuOid.toString() should equal("1.2.246.562.29.00000000000000000009")
    haut(1).hakuOid.toString() should equal("1.2.246.562.29.00000000000000000008")
    var hakukohteet = haut(0).hakukohteet.toArray
    hakukohteet.size should equal(1)
    hakukohteet(0).hakukohdeOid.toString() should equal("1.2.246.562.20.00000000000000000009")
    hakukohteet = haut(1).hakukohteet.toArray
    hakukohteet.size should equal(1)
    hakukohteet(0).hakukohdeOid.toString() should equal("1.2.246.562.20.00000000000000000008")
  }

  it should "should filter out poistetut hakukohteet from hakutiedot" in {
    val sorakuvausId = UUID.randomUUID().toString
    KFT.addSorakuvaus(sorakuvausId, KFT.DefaultSorakuvausScala)
    KFT.addKoulutus("1.2.246.562.13.00000000000000000009", KFT.DefaultKoulutusScala + (KFT.SorakuvausIdKey -> sorakuvausId))
    val toteutusMap = KFT.DefaultToteutusScala + (KFT.KoulutusOidKey -> "1.2.246.562.13.00000000000000000009")
    KFT.addToteutus("1.2.246.562.17.00000000000000000009", toteutusMap)
    KFT.addHaku("1.2.246.562.29.00000000000000000008", KFT.DefaultHakuScala)
    val valintaperusteId = UUID.randomUUID().toString
    KFT.addValintaperuste(valintaperusteId, KFT.DefaultValintaperusteScala)
    val hakukohdeMap = KFT.DefaultHakukohdeScala ++
      Map(KFT.ToteutusOidKey -> "1.2.246.562.17.00000000000000000009", KFT.HakuOidKey -> "1.2.246.562.29.00000000000000000008", KFT.ValintaperusteIdKey -> valintaperusteId)
    KFT.addHakukohde("1.2.246.562.20.00000000000000000008", hakukohdeMap)
    val hakukohdeMap2 = KFT.DefaultHakukohdeScala ++
      Map(KFT.ToteutusOidKey -> "1.2.246.562.17.00000000000000000009", KFT.HakuOidKey -> "1.2.246.562.29.00000000000000000008",
        KFT.ValintaperusteIdKey -> valintaperusteId, KFT.TilaKey -> Poistettu.name)
    KFT.addHakukohde("1.2.246.562.20.00000000000000000009", hakukohdeMap2)

    val hakutieto = read[List[Hakutieto]](KFT.getHakutiedotByKoulutus("1.2.246.562.13.00000000000000000009")).toArray

    hakutieto.size should equal (1)
    hakutieto(0).toteutusOid.toString() should equal ("1.2.246.562.17.00000000000000000009")
    val haut = hakutieto(0).haut.toArray
    haut.size should equal (1)
    haut(0).hakuOid.toString() should equal("1.2.246.562.29.00000000000000000008")
    val hakukohteet = haut(0).hakukohteet.toArray
    hakukohteet.size should equal(1)
    hakukohteet(0).hakukohdeOid.toString() should equal("1.2.246.562.20.00000000000000000008")
  }

  it should "should ignore poistetut toteutukset in hakutiedot" in {
    val sorakuvausId = UUID.randomUUID().toString
    KFT.addSorakuvaus(sorakuvausId, KFT.DefaultSorakuvausScala)
    KFT.addKoulutus("1.2.246.562.13.00000000000000000009", KFT.DefaultKoulutusScala + (KFT.SorakuvausIdKey -> sorakuvausId))
    val toteutusMap = KFT.DefaultToteutusScala + (KFT.KoulutusOidKey -> "1.2.246.562.13.00000000000000000009, KFT.TilaKey -> Poistettu.name")
    KFT.addToteutus("1.2.246.562.17.00000000000000000009", toteutusMap)
    KFT.addHaku("1.2.246.562.29.00000000000000000008", KFT.DefaultHakuScala + (KFT.TilaKey -> Poistettu.name))
    val valintaperusteId = UUID.randomUUID().toString
    KFT.addValintaperuste(valintaperusteId, KFT.DefaultValintaperusteScala)
    val hakukohdeMap = KFT.DefaultHakukohdeScala ++
      Map(KFT.ToteutusOidKey -> "1.2.246.562.17.00000000000000000009", KFT.HakuOidKey -> "1.2.246.562.29.00000000000000000008",
        KFT.ValintaperusteIdKey -> valintaperusteId, KFT.TilaKey -> Poistettu.name)
    KFT.addHakukohde("1.2.246.562.20.00000000000000000008", hakukohdeMap)

    val hakutieto = read[List[Hakutieto]](KFT.getHakutiedotByKoulutus("1.2.246.562.13.00000000000000000009")).toArray

    hakutieto.size should equal (0)
  }
}
