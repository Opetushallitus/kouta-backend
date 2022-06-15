package fi.oph.kouta.integration

import fi.oph.kouta.Templates
import fi.oph.kouta.TestSetups.{CONFIG_PROFILE_TEMPLATE, SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, SYSTEM_PROPERTY_NAME_TEMPLATE}
import fi.oph.kouta.client.{EPerusteKoodiClient, KoulutusKoodiClient, OidAndChildren}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.service.OrganisaatioServiceImpl
import fi.oph.kouta.validation.ammOpeErityisopeJaOpoKoulutusKoodiUrit
import org.json4s.{DefaultFormats, JArray}
import org.json4s.JsonAST.{JField, JString}
import org.json4s.jackson.JsonMethods.parse
import scalacache.caffeine.CaffeineCache
import scalacache.modes.sync.mode
import scalaz.Scalaz.nil

import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt

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

    val client = new KoulutusKoodiClient(null)
    //println("oikea vastaus: " + client.opintojenLaajuusKoodiUriExists(""))

    //val client = new EPerusteKoodiClient(null)
    //println("oikea vastaus: " + client.getOsaamisalaKoodiuritForEPeruste(656))

    println(client.koulutusKoodiUritExist(Seq("koulutus_000001", "koulutus_000002", "koulutus_000003"), Seq("koulutus_000001#12")))
    //val client = new OrganisaatioServiceImpl(null)
    //val all = client.findUnknownOrganisaatioOidsFromHierarkia(Set(OrganisaatioOid("1.2.246.562.10.42788944868"), OrganisaatioOid("1.2.246.562.10.97509708141"), OrganisaatioOid("1.2.246.562.10.53697269479"), OrganisaatioOid("1.2.246.562.10.15816289258")))
    //println(all)
    //println(all.size)
    //println(client.findOrganisaatioOidsFlatByMemberOid(OrganisaatioOid("1.2.246.562.10.54453921329")))
  }
}
