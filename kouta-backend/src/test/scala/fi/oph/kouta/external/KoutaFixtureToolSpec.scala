package fi.oph.kouta.external

import java.util.UUID

import fi.oph.kouta.external.{KoutaFixtureTool => KFT}
import fi.oph.kouta.integration.{AccessControlSpec, EverythingFixture, KoutaIntegrationSpec}
import org.scalatest.BeforeAndAfterEach

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
    val oid = "1.2.246.562.13.00000000000000000009"
    KFT.addKoulutus(oid, KFT.DefaultKoulutus)
    val koulutus = KFT.getKoulutus(oid)
    doPut(KoulutusPath, koulutus, ophHeaders)
  }

  it should "be able to save default toteutus" in {
    KFT.addKoulutus("1.2.246.562.13.00000000000000000009", KFT.DefaultKoulutus)
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
    val tempSorakuvausId = UUID.randomUUID().toString
    KFT.addSorakuvaus(tempSorakuvausId, KFT.DefaultSorakuvausScala)
    val sorakuvaus = KFT.getSorakuvaus(tempSorakuvausId)
    val sorakuvausId = id(doPut(SorakuvausPath, sorakuvaus, ophHeaders)).toString

    val valintaperusteId = UUID.randomUUID().toString
    KFT.addValintaperuste(valintaperusteId, KFT.DefaultValintaperusteScala + (KFT.SorakuvausIdKey -> sorakuvausId))
    val valintaperuste = KFT.getValintaperuste(valintaperusteId)
    doPut(ValintaperustePath, valintaperuste)
  }

  it should "be able to save default hakukohde" in {
    KFT.addKoulutus("1.2.246.562.13.00000000000000000009", KFT.DefaultKoulutus)
    val koulutus = KFT.getKoulutus("1.2.246.562.13.00000000000000000009")
    val koulutusOid = oid(doPut(KoulutusPath, koulutus, ophHeaders))

    val toteutusMap = KFT.DefaultToteutusScala + (KFT.KoulutusOidKey -> koulutusOid)
    KFT.addToteutus("1.2.246.562.17.00000000000000000009", toteutusMap)
    val toteutus = KFT.getToteutus("1.2.246.562.17.00000000000000000009")
    val toteutusOid = oid(doPut(ToteutusPath, toteutus))

    KFT.addHaku("1.2.246.562.29.00000000000000000009", KFT.DefaultHaku)
    val haku = KFT.getHaku("1.2.246.562.29.00000000000000000009")
    val hakuOid = oid(doPut(HakuPath, haku))

    val tempSorakuvausId = UUID.randomUUID().toString
    KFT.addSorakuvaus(tempSorakuvausId, KFT.DefaultSorakuvausScala)
    val sorakuvaus = KFT.getSorakuvaus(tempSorakuvausId)
    val sorakuvausId = id(doPut(SorakuvausPath, sorakuvaus, ophHeaders)).toString

    val tempValintaperusteId = UUID.randomUUID().toString
    KFT.addValintaperuste(tempValintaperusteId, KFT.DefaultValintaperusteScala + (KFT.SorakuvausIdKey -> sorakuvausId))
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

}
