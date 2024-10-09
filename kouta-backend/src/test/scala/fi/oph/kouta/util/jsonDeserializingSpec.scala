package fi.oph.kouta.util

import fi.oph.kouta.domain.{Email, En, Fi, OrgOsoite, OrganisaatioServiceOrg, OrganisaationYhteystieto, Osaamismerkki, Puhelin, Sv, Www}
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
      tyypit = Some(List("organisaatiotyyppi_01")),
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
        tyypit = Some(List("organisaatiotyyppi_01")),
        oppilaitosTyyppiUri = Some("oppilaitostyyppi_63#1"),
        kieletUris = List("oppilaitoksenopetuskieli_1#1", "oppilaitoksenopetuskieli_4#1"))
    }

    assert(parse(organisaatioJson).extract[OrganisaatioServiceOrg] == result)
  }

  "osaamismerkkiSerializer" should "return osaamismerkki with voimassaoloLoppuu value" in {
    val osaamismerkkiJson = "{\"id\" : 9202689," +
      "\"nimi\" : {\"_id\" : \"9204031\"," +
      "\"_tunniste\" : \"f87c9f04-d7e9-4993-a16a-42fd42b8333e\"," +
      "\"fi\" : \"Oma talous\"," +
      "\"sv\" : \"Min ekonomi\"}," +
      "\"kuvaus\" : null," +
      "\"tila\" : \"JULKAISTU\"," +
      "\"kategoria\" : {\"id\" : 9202621," +
      "\"nimi\" : {\"_id\" : \"9202526\"," +
      "\"_tunniste\" : \"0a2fbef2-0549-4a43-81f6-4ecfb447b091\"," +
      "\"fi\" : \"Numero- ja taloustaidot\"," +
      "\"sv\" : \"Numeriska färdigheter och färdigheter i ekonomi\" }," +
      "\"kuvaus\" : null," +
      "\"liite\" : {\"id\" : \"a0dcad65-088c-480c-a116-977ecbbe52f7\"," +
      "\"nimi\" : \"numero_ja_taloustaidot_eitekstia.png\"," +
      "\"mime\" : \"image/png\"," +
      "\"binarydata\" : \"iVBORw0KGgoAAAANSUhEUgAAAM\"}," +
      "\"muokattu\" : 1727934803353}," +
      "\"koodiUri\" : \"osaamismerkit_1010\"," +
      "\"osaamistavoitteet\" : [{\"id\" : 9202891," +
      "\"osaamistavoite\" : {\"_id\" : \"9204032\"," +
      "\"_tunniste\" : \"9a625d8d-7fec-4d8f-9a6f-6aa5f44331f0\"," +
      "\"fi\" : \"osaa tunnistaa menojen ja tulojen vaikutuksen taloudelliseen tilanteeseensa\"," +
      "\"sv\" : \"är medveten om hur utgifter och inkomster inverkar på den egna ekonomiska situationen\"}}]," +
      "\"arviointikriteerit\" : [ {\"id\" : 9202873," +
      "\"arviointikriteeri\" : {\"_id\" : \"9204015\"," +
          "\"_tunniste\" : \"bd867103-ad5f-4640-93ea-54933d10ae3c\"," +
          "\"fi\" : \"erittelee oman taloutensa menot ja tulot\"," +
          "\"sv\" : \"specificera utgifter och inkomster för sin egen ekonomi\"}}]," +
      "\"voimassaoloAlkaa\" : 1704060000000," +
      "\"voimassaoloLoppuu\" : 1727643600000," +
      "\"muokattu\" : 1706787214743," +
      "\"muokkaaja\" : \"1.2.246.562.24.16945731101\"}"

    assert(parse(osaamismerkkiJson).extract[Osaamismerkki] == Osaamismerkki(tila = "JULKAISTU", koodiUri = "osaamismerkit_1010", voimassaoloLoppuu = Some(1727643600000L)))
  }

  "osaamismerkkiSerializer" should "return osaamismerkki without voimassaoloLoppuu" in {
    val osaamismerkkiJson = "{\"id\" : 9202689," +
      "\"nimi\" : {\"_id\" : \"9204031\"," +
      "\"_tunniste\" : \"f87c9f04-d7e9-4993-a16a-42fd42b8333e\"," +
      "\"fi\" : \"Oma talous\"," +
      "\"sv\" : \"Min ekonomi\"}," +
      "\"kuvaus\" : null," +
      "\"tila\" : \"JULKAISTU\"," +
      "\"kategoria\" : {\"id\" : 9202621," +
      "\"nimi\" : {\"_id\" : \"9202526\"," +
      "\"_tunniste\" : \"0a2fbef2-0549-4a43-81f6-4ecfb447b091\"," +
      "\"fi\" : \"Numero- ja taloustaidot\"," +
      "\"sv\" : \"Numeriska färdigheter och färdigheter i ekonomi\" }," +
      "\"kuvaus\" : null," +
      "\"liite\" : {\"id\" : \"a0dcad65-088c-480c-a116-977ecbbe52f7\"," +
      "\"nimi\" : \"numero_ja_taloustaidot_eitekstia.png\"," +
      "\"mime\" : \"image/png\"," +
      "\"binarydata\" : \"iVBORw0KGgoAAAANSUhEUgAAAM\"}," +
      "\"muokattu\" : 1727934803353}," +
      "\"koodiUri\" : \"osaamismerkit_1010\"," +
      "\"osaamistavoitteet\" : [{\"id\" : 9202891," +
      "\"osaamistavoite\" : {\"_id\" : \"9204032\"," +
      "\"_tunniste\" : \"9a625d8d-7fec-4d8f-9a6f-6aa5f44331f0\"," +
      "\"fi\" : \"osaa tunnistaa menojen ja tulojen vaikutuksen taloudelliseen tilanteeseensa\"," +
      "\"sv\" : \"är medveten om hur utgifter och inkomster inverkar på den egna ekonomiska situationen\"}}]," +
      "\"arviointikriteerit\" : [ {\"id\" : 9202873," +
      "\"arviointikriteeri\" : {\"_id\" : \"9204015\"," +
      "\"_tunniste\" : \"bd867103-ad5f-4640-93ea-54933d10ae3c\"," +
      "\"fi\" : \"erittelee oman taloutensa menot ja tulot\"," +
      "\"sv\" : \"specificera utgifter och inkomster för sin egen ekonomi\"}}]," +
      "\"voimassaoloAlkaa\" : 1704060000000," +
      "\"voimassaoloLoppuu\" : null," +
      "\"muokattu\" : 1706787214743," +
      "\"muokkaaja\" : \"1.2.246.562.24.16945731101\"}"

    assert(parse(osaamismerkkiJson).extract[Osaamismerkki] == Osaamismerkki(tila = "JULKAISTU", koodiUri = "osaamismerkit_1010", voimassaoloLoppuu = None))
  }
}

