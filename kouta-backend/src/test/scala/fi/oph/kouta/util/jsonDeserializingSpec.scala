package fi.oph.kouta.util

import fi.oph.kouta.domain.{Email, En, Fi, OrgOsoite, OrganisaatioServiceOrg, OrganisaationYhteystieto, Puhelin, Sv, Www}
import org.json4s.jackson.JsonMethods.parse

class jsonDeserializingSpec extends UnitSpec with KoutaJsonFormats {

  "organisaationYhteystietoSerializer" should "return empty list for yhteystiedot" in {
    assert(parse("[]").extract[List[OrganisaationYhteystieto]] == List())
  }

  it should "return kayntiosoite for yhteystiedot" in {
    val organisaationYhteystiedotJson = "[" +
        "{\"osoiteTyyppi\":\"kaynti\",\"kieli\":\"kieli_fi#1\",\"postinumeroUri\":\"posti_00920\",\"yhteystietoOid\":\"1.2.246.562.5.33561261158\",\"id\":\"3263989\",\"postitoimipaikka\":\"HELSINKI\",\"osoite\":\"Matomäenranta 985\",\"ytjPaivitysPvm\":\"2015-06-25\"}" +
      "]"

    assert(parse(organisaationYhteystiedotJson).extract[List[OrganisaationYhteystieto]] == List(OrgOsoite(osoiteTyyppi = "kaynti", kieli = Fi, osoite = "Matomäenranta 985", postinumeroUri = Some("posti_00920"))))
  }

  it should "return null for yhteystieto with ulkomainen_kaynti as osoiteTyyppi" in {
    val organisaationYhteystiedotJson = "[" +
      "{\"osoiteTyyppi\":\"ulkomainen_kaynti\",\"kieli\":\"kieli_en#1\",\"yhteystietoOid\":\"1.2.246.562.5.2014050813493375734386\",\"id\":\"3263980\",\"osoite\":\"Kärpäsenkuja 219\"}]," +
      "]"

    assert(parse(organisaationYhteystiedotJson).extract[List[OrganisaationYhteystieto]] == List(null))
  }

  "organisaatio serializer" should "return empty list for organisaation yhteystiedot" in {
    val organisaatioJson = "{\"maskingActive\":false," +
      "\"tyypit\":[\"organisaatiotyyppi_01\"]," +
      "\"nimi\":{\"fi\":\"Organisaatio\",\"sv\":\"Organisation\",\"en\":\"Organization\"}," +
      "\"nimet\":[{\"nimi\":{\"fi\":\"Organisaatio\",\"sv\":\"Organisation\",\"en\":\"Organization\"}," +
      "\"alkuPvm\":\"2007-02-23\"," +
      "\"version\":0}]," +
      "\"kuvaus2\":{}," +
      "\"oid\":\"1.2.246.562.10.60198812368\"," +
      "\"yhteystiedot\":[]," +
      "\"kayntiosoite\":{\"postinumeroUri\":\"posti_00920\",\"osoiteTyyppi\":\"kaynti\",\"yhteystietoOid\":\"1.2.246.562.5.33561261158\",\"postitoimipaikka\":\"HELSINKI\",\"osoite\":\"Matomäenranta 985\",\"ytjPaivitysPvm\":\"2015-06-25\"}," +
      "\"postiosoite\":{\"postinumeroUri\":\"posti_00920\",\"osoiteTyyppi\":\"posti\",\"yhteystietoOid\":\"1.2.246.562.5.53115405465\",\"postitoimipaikka\":\"HELSINKI\",\"osoite\":\"Myllypurontie 1\",\"ytjPaivitysPvm\":\"2023-11-19\"}," +
      "\"kieletUris\":[\"oppilaitoksenopetuskieli_1#1\",\"oppilaitoksenopetuskieli_4#1\"]," +
      "\"kotipaikkaUri\":\"kunta_091\"," +
      "\"parentOidPath\":\"|1.2.246.562.10.00000000001|\"," +
      "\"toimipistekoodi\":\"\"," +
      "\"lyhytNimi\":{\"fi\":\"Organisaatio\",\"sv\":\"Organisation\",\"en\":\"Organization\"}," +
      "\"status\":\"AKTIIVINEN\"," +
      "\"oppilaitosTyyppiUri\": \"oppilaitostyyppi_63#1\"}"

    assert(parse(organisaatioJson).extract[OrganisaatioServiceOrg] == OrganisaatioServiceOrg(
      oid = "1.2.246.562.10.60198812368",
      parentOidPath = "|1.2.246.562.10.00000000001|",
      nimi = Map(Fi -> "Organisaatio", Sv -> "Organisation", En -> "Organization"),
      yhteystiedot = Some(List()),
      kotipaikkaUri = Some("kunta_091"),
      status = "AKTIIVINEN",
      tyypit = List("organisaatiotyyppi_01"),
      oppilaitosTyyppiUri = Some("oppilaitostyyppi_63#1"),
      kieletUris = List("oppilaitoksenopetuskieli_1#1", "oppilaitoksenopetuskieli_4#1")
    ))
  }

  it should "return organisaatio with all yhteystiedot" in {
    val organisaatioJson = "{\"maskingActive\":false," +
      "\"tyypit\":[\"organisaatiotyyppi_01\"]," +
      "\"nimi\":{\"fi\":\"Organisaatio\",\"sv\":\"Organisation\",\"en\":\"Organization\"}," +
      "\"nimet\":[{\"nimi\":{\"fi\":\"Organisaatio\",\"sv\":\"Organisation\",\"en\":\"Organization\"}," +
      "\"alkuPvm\":\"2007-02-23\"," +
      "\"version\":0}]," +
      "\"kuvaus2\":{}," +
      "\"oid\":\"1.2.246.562.10.60198812368\"," +
      "\"yhteystiedot\":[" +
      "{\"kieli\":\"kieli_fi#1\",\"yhteystietoOid\":\"1.2.246.562.5.92268146039\",\"id\":\"3263987\",\"email\":\"kirjaamo@organisaatio.fi\"}," +
      "{\"kieli\":\"kieli_en#1\",\"numero\":\"044 0909090\",\"tyyppi\":\"puhelin\",\"yhteystietoOid\":\"1.2.246.562.5.2014050813493375751756\",\"id\":\"3263981\"}," +
      "{\"osoiteTyyppi\":\"ulkomainen_posti\",\"kieli\":\"kieli_en#1\",\"yhteystietoOid\":\"1.2.246.562.5.2014050813493375746317\",\"id\":\"3263985\",\"osoite\":\"Yläpääntie 734\"}," +
      "{\"kieli\":\"kieli_en#1\",\"www\":\"http://www.organisaatio.fi\",\"yhteystietoOid\":\"13995461961930.42250296518893804\",\"id\":\"3263983\"}," +
      "{\"kieli\":\"kieli_fi#1\",\"www\":\"http://www.organisaatio.fi\",\"yhteystietoOid\":\"1.2.246.562.5.28418853389\",\"id\":\"3263984\"}," +
      "{\"osoiteTyyppi\":\"posti\",\"kieli\":\"kieli_fi#1\",\"postinumeroUri\":\"posti_00920\",\"yhteystietoOid\":\"1.2.246.562.5.53115405465\",\"id\":\"3263982\",\"postitoimipaikka\":\"HELSINKI\",\"osoite\":\"Myllypurontie 1\",\"ytjPaivitysPvm\":\"2023-11-19\"}," +
      "{\"osoiteTyyppi\":\"kaynti\",\"kieli\":\"kieli_fi#1\",\"postinumeroUri\":\"posti_00920\",\"yhteystietoOid\":\"1.2.246.562.5.33561261158\",\"id\":\"3263989\",\"postitoimipaikka\":\"HELSINKI\",\"osoite\":\"Matomäenranta 985\",\"ytjPaivitysPvm\":\"2015-06-25\"}," +
      "{\"kieli\":\"kieli_fi#1\",\"numero\":\"09 74245555\",\"tyyppi\":\"puhelin\",\"yhteystietoOid\":\"1.2.246.562.5.252058358510\",\"id\":\"3263986\"}," +
      "{\"kieli\":\"kieli_en#1\",\"yhteystietoOid\":\"13995461737560.9465107542673301\",\"id\":\"3263988\",\"email\":\"hakija-60428052@oph.fi\"}," +
      "{\"osoiteTyyppi\":\"ulkomainen_kaynti\",\"kieli\":\"kieli_en#1\",\"yhteystietoOid\":\"1.2.246.562.5.2014050813493375734386\",\"id\":\"3263980\",\"osoite\":\"Kärpäsenkuja 219\"}]," +
      "\"kayntiosoite\":{\"postinumeroUri\":\"posti_00920\",\"osoiteTyyppi\":\"kaynti\",\"yhteystietoOid\":\"1.2.246.562.5.33561261158\",\"postitoimipaikka\":\"HELSINKI\",\"osoite\":\"Matomäenranta 985\",\"ytjPaivitysPvm\":\"2015-06-25\"}," +
      "\"postiosoite\":{\"postinumeroUri\":\"posti_00920\",\"osoiteTyyppi\":\"posti\",\"yhteystietoOid\":\"1.2.246.562.5.53115405465\",\"postitoimipaikka\":\"HELSINKI\",\"osoite\":\"Myllypurontie 1\",\"ytjPaivitysPvm\":\"2023-11-19\"}," +
      "\"kieletUris\":[\"oppilaitoksenopetuskieli_1#1\",\"oppilaitoksenopetuskieli_4#1\"]," +
      "\"kotipaikkaUri\":\"kunta_091\"," +
      "\"parentOidPath\":\"|1.2.246.562.10.00000000001|\"," +
      "\"toimipistekoodi\":\"\"," +
      "\"lyhytNimi\":{\"fi\":\"Organisaatio\",\"sv\":\"Organisation\",\"en\":\"Organization\"}," +
      "\"status\":\"AKTIIVINEN\", " +
      "\"oppilaitosTyyppiUri\": \"oppilaitostyyppi_63#1\"}"

    val result = {
      OrganisaatioServiceOrg(oid = "1.2.246.562.10.60198812368",
        parentOidPath = "|1.2.246.562.10.00000000001|",
        nimi = Map(Fi -> "Organisaatio", Sv -> "Organisation", En -> "Organization"),
        yhteystiedot = Some(List(
          Email(kieli = Fi, email = "kirjaamo@organisaatio.fi"),
          Puhelin(kieli = En, numero = "044 0909090"),
          null,
          Www(kieli = En, www = "http://www.organisaatio.fi"),
          Www(kieli = Fi, www = "http://www.organisaatio.fi"),
          OrgOsoite(osoiteTyyppi = "posti", kieli = Fi, osoite = "Myllypurontie 1", postinumeroUri = Some("posti_00920")),
          OrgOsoite(osoiteTyyppi = "kaynti", kieli = Fi, osoite = "Matomäenranta 985", postinumeroUri = Some("posti_00920")),
          Puhelin(kieli = Fi, numero = "09 74245555"),
          Email(kieli = En, email = "hakija-60428052@oph.fi"),
          null
        )),
        status = "AKTIIVINEN",
        kotipaikkaUri = Some("kunta_091"),
        tyypit = List("organisaatiotyyppi_01"),
        oppilaitosTyyppiUri = Some("oppilaitostyyppi_63#1"),
        kieletUris = List("oppilaitoksenopetuskieli_1#1", "oppilaitoksenopetuskieli_4#1"))
    }

    assert(parse(organisaatioJson).extract[OrganisaatioServiceOrg] == result)
  }
}
