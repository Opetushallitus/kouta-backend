package fi.oph.kouta

import fi.oph.kouta.domain.{Julkaisutila, Koulutus, Koulutustyyppi}
import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class KoutaBackendSwagger extends Swagger(
  Swagger.SpecVersion,
  "0.1-SNAPSHOT",
  ApiInfo("kouta-backend",
    "Uusi tarjonta",
    "https://opintopolku.fi/wp/fi/opintopolku/tietoa-palvelusta/",
    "verkkotoimitus_opintopolku@oph.fi",
    "EUPL 1.1 or latest approved by the European Commission",
    "http://www.osor.eu/eupl/"))

class SwaggerServlet(implicit val swagger: Swagger) extends ScalatraServlet with JacksonSwaggerBase

trait SwaggerModels {

  private def mp(t:DataType = DataType.String, required:Boolean = true, description:Option[String] = None) =
    ModelProperty(`type` = t, required = required, description = description)

  val KoulutusModel = Model(
    id = classOf[Koulutus].getSimpleName,
    name = classOf[Koulutus].getSimpleName,
    properties = List(
      "oid" -> mp(required = false, description = Some("Päivitettävän koulutuksen oid")),
      "johtaaTutkintoon" -> mp(t = DataType.Boolean),
      "koulutustyyppi" -> mp(description = Some("amm/kk/lk/muu")),
      "koulutusKoodiUri" -> mp(description = Some("koulutuskoodiuri_koodi#versio")),
      "tila" -> mp(description=Some("tallennettu|julkaistu|arkistoitu")),
      "tarjoajat" -> mp( t = DataType.GenList(DataType.String), description=Some("Lista organisaatioiden oideja")),
      "nimi" -> mp( t = DataType.GenMap(DataType.String), description = Some("(fi -> \"nimi fi\", sv -> \"nimi sv\")")),
      "kuvaus" -> mp( t = DataType.GenMap(DataType.String), description = Some("(fi -> \"kuvaus fi\", sv -> \"kuvaus sv\")")),
      "muokkaaja" -> mp(description = Some("Muokkaajan oid"))))
}