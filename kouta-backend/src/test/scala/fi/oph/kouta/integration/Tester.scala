package fi.oph.kouta.integration

import fi.oph.kouta.Templates
import fi.oph.kouta.TestSetups.{CONFIG_PROFILE_TEMPLATE, SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, SYSTEM_PROPERTY_NAME_TEMPLATE}
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain.{Hakukohde, Julkaistu}
import fi.oph.kouta.repository.HakukohdeDAO
import org.json4s.jackson.JsonMethods.parse

import java.util.UUID

object Tester {
  def main(args: Array[String]): Unit = {
    //implicit val formats       = DefaultFormats
    case class AliIiittemi(aliItemi: String)
    case class Iittemi(itemi: String, itemi3: AliIiittemi)

    val json = "[{\"itemi\": \"foo\", \"itemi2\": \"bar\", \"itemi3\": {\"aliItemi\": \"ali-foo\", \"ali-itemi2\": \"ali-bar\"}}, {\"itemi\": \"foo2\", \"itemi2\": \"bar2\", \"itemi3\": {\"aliItemi\": \"ali-foo2\", \"ali-itemi2\": \"ali-bar2\"}}]"
    val jsonVal = parse(json)
    //val jsonArr = jsonVal.extract[List[Iittemi]]

    System.setProperty(SYSTEM_PROPERTY_NAME_TEMPLATE, Templates.DEFAULT_TEMPLATE_FILE_PATH)
    System.setProperty(SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, CONFIG_PROFILE_TEMPLATE)

    //HakemusPalveluClient.isExistingAtaruId(UUID.fromString("0c3e3cc6-8892-4b82-8663-0c2bc79c02bb"))
    //val client = new HakuKoodiClient(null)
    //println("oikea vastaus: " + client.hakukohdeKoodiUriExists("hakukohteetperusopetuksenjalkeinenyhteishaku_666"))
    //println("oikea vastaus: " + client.opintojenLaajuusKoodiUriExists(""))
    //println(HakukohdeDAO.get(HakukohdeOid("1.2.246.562.20.00000000000000016744"), TilaFilter.onlyOlemassaolevat()))
    val hk = Hakukohde(organisaatioOid=OrganisaatioOid("1.2.246.562.10.13803815193"), oid = Some(HakukohdeOid("1.2.246.562.20.00000000000000016744")), toteutusOid = ToteutusOid("1.2.246.562.17.00000000000000000417"),
      hakuOid = HakuOid("1.2.246.562.29.00000000000000012989"), tila = Julkaistu, muokkaaja = UserOid("1.2.246.562.24.41529289358"), valintaperusteId = Some(UUID.fromString("f9c68838-2fe8-4333-a8fd-599835d9ae79")))
    println(HakukohdeDAO.getDependencyInformation(hk))
    //HakukohdeDAO.getDependencyInformation(JulkaistuHakukohde.copy(oid = Some(HakukohdeOid("1.2.246.562.20.00000000000000004244")), toteutusOid = ToteutusOid("1.2.246.562.17.00000000000000003366"),
    //  hakuOid = HakuOid("1.2.246.562.29.00000000000000002821"), valintaperusteId = Some(UUID.fromString("0c3e3cc6-8892-4b82-8663-0c2bc79c02bb")))).get("1.2.246.562.17.00000000000000003366").get._4.get.foreach(println(_))


    //println(client.koulutusKoodiUritExist(Seq("koulutus_000001", "koulutus_000002", "koulutus_000003"), Seq("koulutus_000001#12")))
    //val client = new OrganisaatioServiceImpl(null)
    //val all = client.findUnknownOrganisaatioOidsFromHierarkia(Set(OrganisaatioOid("1.2.246.562.10.42788944868"), OrganisaatioOid("1.2.246.562.10.97509708141"), OrganisaatioOid("1.2.246.562.10.53697269479"), OrganisaatioOid("1.2.246.562.10.15816289258")))
    //println(all)
    //println(all.size)
    //println(client.findOrganisaatioOidsFlatByMemberOid(OrganisaatioOid("1.2.246.562.10.54453921329")))
  }
}
