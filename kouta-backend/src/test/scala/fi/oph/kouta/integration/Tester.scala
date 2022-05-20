package fi.oph.kouta.integration

import fi.oph.kouta.client.{EPerusteKoodiClient, KoulutusKoodiClient}
import org.json4s.{DefaultFormats, JArray}
import org.json4s.JsonAST.{JField, JString}
import org.json4s.jackson.JsonMethods.parse
import scalacache.caffeine.CaffeineCache
import scalacache.modes.sync.mode

object Tester {
  def main(args: Array[String]): Unit = {
    //implicit val formats       = DefaultFormats
    case class AliIiittemi(aliItemi: String)
    case class Iittemi(itemi: String, itemi3: AliIiittemi)

    val json = "[{\"itemi\": \"foo\", \"itemi2\": \"bar\", \"itemi3\": {\"aliItemi\": \"ali-foo\", \"ali-itemi2\": \"ali-bar\"}}, {\"itemi\": \"foo2\", \"itemi2\": \"bar2\", \"itemi3\": {\"aliItemi\": \"ali-foo2\", \"ali-itemi2\": \"ali-bar2\"}}]"
    val jsonVal = parse(json)
    //val jsonArr = jsonVal.extract[List[Iittemi]]

    println(System.currentTimeMillis())
    //val client = new KoulutusKoodiClient(null)
    //println("oikea vastaus: " + client.ammatillinenKoulutusKoodiUriExists("koulutus_11111"))

    val client = new EPerusteKoodiClient(null)
    println("oikea vastaus: " + client.ePerusteIdValidForKoulutusKoodiUrit(666, Seq("koulutus_361104#6")))

  }
}
