package fi.oph.kouta.client

import org.scalatra.test.scalatest.ScalatraFlatSpec

import java.util.UUID

class HakemusPalveluClientSpec extends ScalatraFlatSpec {
  "Parsing active forms" should "return forms in list" in {
    val responseString =
      "{\"forms\": [{\"deleted\":null,\"key\":\"51666769-c664-40a8-893c-7bbe82399eea\",\"content\":[],\"name\":{\"fi\":\"Dummylomake - KOPIO\"}," +
        "\"created-by\":\"somebody\",\"locked\":null,\"id\":950454,\"created-time\":\"2022-08-16T13:53:36.708Z\",\"languages\":[\"fi\"]}," +
        "{\"deleted\":false,\"key\":\"837d728c-359e-4ad0-b61b-151bb790cb5b\",\"content\":[],\"name\":{\"fi\":\"Korkeakoulujen kevään 2022 toinen yhteishaku\"," +
        "\"sv\":\"Högskolornas andra gemensamma ansökan, våren 2022\",\"en\":\"Joint Application to Degree Programmes in Finnish/Swedish, Spring 2022\"}," +
        "\"created-by\":\"somebody\",\"locked\": null,\"id\":950453,\"created-time\":\"2022-03-10T11:53:38.532Z\",\"languages\":[\"fi\",\"sv\",\"en\"]}]}"
    HakemusPalveluClient.parseForms(responseString) should equal(
      Seq(AtaruForm("51666769-c664-40a8-893c-7bbe82399eea", None, None),
        AtaruForm("837d728c-359e-4ad0-b61b-151bb790cb5b", Some(false), None)
      )
    )
  }

  it should "exclude inactive forms from list" in {
    val responseString =
      "{\"forms\": [{\"deleted\":null,\"key\":\"51666769-c664-40a8-893c-7bbe82399eea\",\"content\":[],\"name\":{\"fi\":\"Dummylomake - KOPIO\"}," +
        "\"created-by\":\"somebody\",\"locked\":null,\"id\":950454,\"created-time\":\"2022-08-16T13:53:36.708Z\",\"languages\":[\"fi\"]," +
        "\"properties\":{\"allowOnlyYhteisHaut\":false}}," +
        "{\"deleted\":true,\"key\":\"837d728c-359e-4ad0-b61b-151bb790cb5b\",\"content\":[],\"name\":{\"fi\":\"Korkeakoulujen kevään 2022 toinen yhteishaku\"," +
        "\"sv\":\"Högskolornas andra gemensamma ansökan, våren 2022\",\"en\":\"Joint Application to Degree Programmes in Finnish/Swedish, Spring 2022\"}," +
        "\"created-by\":\"somebody\",\"locked\": null,\"id\":950453,\"created-time\":\"2022-03-10T11:53:38.532Z\",\"languages\":[\"fi\",\"sv\",\"en\"]}," +
        "{\"deleted\":null,\"key\":\"ad6329c3-79c5-4996-984e-d4b76d7eb129\",\"content\":[],\"name\":{\"fi\":\"Salpaus - jatkuva haku 2021\"}," +
        "\"created-by\":\"somebody\",\"locked\":\"2022-04-16T08:46:43.209Z\",\"id\":771482,\"created-time\":\"2021-03-16T08:46:43.209Z\",\"languages\":[\"fi\"]}]}"
    HakemusPalveluClient.parseForms(responseString) should equal(
      Seq(AtaruForm("51666769-c664-40a8-893c-7bbe82399eea", None, Some(AtaruFormProperties(Some(false)))),
        AtaruForm("ad6329c3-79c5-4996-984e-d4b76d7eb129", None, None))
    )
  }

  "Parsing empty response" should "return empty list" in {
    HakemusPalveluClient.parseForms("{\"forms\": []}") should equal(Seq())
  }

  "Form with no properties" should "allow any hakutapa" in {
    val form = AtaruForm("51666769-c664-40a8-893c-7bbe82399eea", None, None);
    HakemusPalveluClient.formAllowsHakuTapa(form, Some("hakutapakoodiuri")) should equal(true);
  }

  "Form with property allowOnlyYhteishaku" should "not allow any hakutapa" in {
    val form = AtaruForm("51666769-c664-40a8-893c-7bbe82399eea", None, Some(AtaruFormProperties(Some(true))));
    HakemusPalveluClient.formAllowsHakuTapa(form, Some("hakutapakoodiuri")) should equal(false);
  }

  "Form with property allowOnlyYhteishaku" should "allow yhteishaku hakutapa" in {
    val form = AtaruForm("51666769-c664-40a8-893c-7bbe82399eea", None, Some(AtaruFormProperties(Some(true))));
    HakemusPalveluClient.formAllowsHakuTapa(form, Some("hakutapa_01")) should equal(true);
  }

  "defaultParse" should "work with HaukohdeApplicationCounts" in {
    HakemusPalveluClient.defaultParse[HakemusPalveluClient.HakukohdeApplicationCounts]("{\"12345\": 123}") should equal(Map("12345" -> 123));
  }
}
