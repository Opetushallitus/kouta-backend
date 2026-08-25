package fi.oph.kouta.elasticsearch

import com.sksamuel.elastic4s.{ElasticClient, Hit}
import fi.oph.kouta.domain.{HakuSearchItemFromIndex, TarkkaAlkamisajankohta}
import fi.oph.kouta.logging.Logging
import fi.oph.kouta.util.{KoutaJsonFormats, UnitSpec}
import org.json4s.jackson.JsonMethods.parse
import org.scalatest.Inspectors.forEvery

class ElasticsearchClientSpec extends UnitSpec with KoutaJsonFormats with Logging {

  private object TestElasticsearchClient extends ElasticsearchClient with KoutaJsonFormats with Logging {
    override val client: ElasticClient = null
  }

  import TestElasticsearchClient.koutaHitReader

  private case class TestHit(sourceAsString: String, id: String = "1.2.246.562.29.00000000000000000009") extends Hit {
    override def index: String                    = "haku-kouta-virkailija"
    override def version: Long                    = 1
    override def seqNo: Long                      = 1
    override def primaryTerm: Long                = 1
    override def sort: Option[Seq[AnyRef]]        = None
    override def sourceAsMap: Map[String, AnyRef] = Map()
    override def exists: Boolean                  = true
    override def score: Float                     = 1.0f
  }

  private val HakutapaOk =
    """"hakutapa": { "koodiUri": "hakutapa_03#1", "nimi": { "fi": "Jatkuva haku" } }"""

  // Tarkka alkamisajankohta: indeksoija kirjoittaa käyttämättömät kentät literaaleina nulleina.
  private val TarkkaAlkamisajankohtaMetadata =
    """,
      |  "metadata": {
      |    "koulutuksenAlkamiskausi": {
      |      "alkamiskausityyppi": "tarkka alkamisajankohta",
      |      "henkilokohtaisenSuunnitelmanLisatiedot": {},
      |      "koulutuksenAlkamiskausi": null,
      |      "koulutuksenAlkamispaivamaara": "2024-09-01T00:00",
      |      "koulutuksenPaattymispaivamaara": null,
      |      "formatoituKoulutuksenalkamispaivamaara": null,
      |      "formatoituKoulutuksenpaattymispaivamaara": null,
      |      "koulutuksenAlkamiskausiKoodiUri": null,
      |      "koulutuksenAlkamisvuosi": null
      |    }
      |  }""".stripMargin

  // Kouta-indeksoijan kirjoittama haku-dokumentti.
  private def hakuJson(metadataField: String, hakutapaField: String = HakutapaOk, nimiField: String = """"nimi": { "fi": "Hassu haku", "sv": "Rolig ansökan" }""") =
    s"""{
       |  "oid": "1.2.246.562.29.00000000000000000009",
       |  $nimiField,
       |  "organisaatio": {
       |    "oid": "1.2.246.562.10.594252633210",
       |    "nimi": { "fi": "Organisaatio fi", "sv": "Organisaatio sv" }
       |  },
       |  "muokkaaja": { "nimi": "Testi Muokkaaja", "oid": "1.2.246.562.24.62301161440" },
       |  "modified": "2024-05-25T10:00:00",
       |  "tila": "julkaistu",
       |  $hakutapaField,
       |  "hakukohteet": [
       |    {
       |      "oid": "1.2.246.562.20.00000000000000000009",
       |      "nimi": { "fi": "Hakukohde fi" },
       |      "tila": "julkaistu",
       |      "modified": "2024-05-25T10:00:00",
       |      "organisaatio": {
       |        "oid": "1.2.246.562.10.594252633210",
       |        "nimi": { "fi": "Organisaatio fi" }
       |      }
       |    }
       |  ]$metadataField
       |}""".stripMargin

  private def read(json: String) =
    TestElasticsearchClient.koutaHitReader[HakuSearchItemFromIndex].read(TestHit(json))

  "koutaHitReader" should "deserialize an indexed haku whose alkamiskausi fields are literal nulls" in {
    val hit = read(hakuJson(TarkkaAlkamisajankohtaMetadata))

    withClue(hit) { hit.isSuccess should be(true) }
    val item = hit.get
    item.oid.s should be("1.2.246.562.29.00000000000000000009")
    item.metadata.koulutuksenAlkamiskausi.alkamiskausityyppi should be(Some(TarkkaAlkamisajankohta))
    item.metadata.koulutuksenAlkamiskausi.koulutuksenAlkamiskausi should be(None)
    item.hakukohteet.length should be(1)
  }

  // Nämä dokumentit katosivat hakutuloksista json4s-päivityksen jälkeen: haku jäi pois siltä
  // sivulta, jolle se osui, vaikka totalCount laski sen mukaan.
  it should "deserialize an indexed haku that has no koulutuksenAlkamiskausi at all" in {
    val metadataVariants = Seq(
      "metadata null"                         -> ""","metadata": null""",
      "koulutuksenAlkamiskausi null"          -> ""","metadata": { "koulutuksenAlkamiskausi": null }""",
      "koulutuksenAlkamiskausi tyhjä objekti" -> ""","metadata": { "koulutuksenAlkamiskausi": {} }""",
      "metadata puuttuu"                      -> ""
    )

    forEvery(metadataVariants) { case (name, metadataField) =>
      val hit = read(hakuJson(metadataField))
      withClue(s"$name: $hit") { hit.isSuccess should be(true) }
      hit.get.metadata.koulutuksenAlkamiskausi.alkamiskausityyppi should be(None)
    }
  }

  // Json4s 3.6 palautti literaalista nullista Map.empty. Oletusarvot + TreatAsAbsent palauttavat
  // saman käyttäytymisen, jotta haku ei katoa pelkän puuttuvan käännöksen takia.
  it should "deserialize an indexed haku whose Kielistetty fields are literal nulls" in {
    val nullKielistetty = Seq(
      "nimi null"                -> hakuJson(TarkkaAlkamisajankohtaMetadata, nimiField = """"nimi": null"""),
      "organisaatio.nimi null"   -> hakuJson(TarkkaAlkamisajankohtaMetadata).replace(""""nimi": { "fi": "Organisaatio fi", "sv": "Organisaatio sv" }""", """"nimi": null"""),
      "hakutapa.nimi null"       -> hakuJson(TarkkaAlkamisajankohtaMetadata, hakutapaField = """"hakutapa": { "koodiUri": "hakutapa_03#1", "nimi": null }"""),
      "hakukohteen nimi null"    -> hakuJson(TarkkaAlkamisajankohtaMetadata).replace(""""nimi": { "fi": "Hakukohde fi" }""", """"nimi": null""")
    )

    forEvery(nullKielistetty) { case (name, json) =>
      val hit = read(json)
      withClue(s"$name: $hit") { hit.isSuccess should be(true) }
    }
  }

  // Tunnistetiedot jätetään tarkoituksella ilman oletusarvoa: niiden puuttuminen on datavirhe, joka
  // halutaan nähdä lokista pudotettuna dokumenttina eikä peitellä tyhjällä tekaistulla arvolla.
  it should "drop a document that is missing mandatory identifying fields" in {
    val invalid = Seq(
      "hakutapa null"     -> hakuJson(TarkkaAlkamisajankohtaMetadata, hakutapaField = """"hakutapa": null"""),
      "organisaatio null" -> hakuJson(TarkkaAlkamisajankohtaMetadata).replace(
        """"organisaatio": {
          |    "oid": "1.2.246.562.10.594252633210",
          |    "nimi": { "fi": "Organisaatio fi", "sv": "Organisaatio sv" }
          |  }""".stripMargin, """"organisaatio": null""")
    )

    forEvery(invalid) { case (name, json) =>
      withClue(s"$name") { read(json).isFailure should be(true) }
    }
  }

  // Dokumentoi, miksi indeksidokumentit luetaan omilla formaateillaan: sovelluksen tiukat formaatit
  // hylkäävät dokumentin, jossa on literaaleja nulleja Kielistetty-kentissä (json4s 4.0:n
  // strictMapExtraction). Jos tämä testi alkaa epäonnistua, json4s:n käyttäytyminen on muuttunut ja
  // GenericKoutaFormats.indexedDocumentFormats voi olla tarpeeton.
  it should "not be readable with the application's strict formats" in {
    assertThrows[org.json4s.MappingException] {
      parse(hakuJson(TarkkaAlkamisajankohtaMetadata)).extract[HakuSearchItemFromIndex]
    }
  }

  "toSearchResult" should "keep the documents it can read, drop the rest and leave totalCount alone" in {
    val good   = TestHit(hakuJson(TarkkaAlkamisajankohtaMetadata), id = "haku-ok")
    val broken = TestHit("""{ "oid": "1.2.246.562.29.00000000000000000001" }""", id = "haku-rikki")

    val result = TestElasticsearchClient.toSearchResult[HakuSearchItemFromIndex](totalCount = 42, hits = Seq(good, broken, good))

    result.result.length should be(2)
    result.totalCount should be(42)
  }
}
