package fi.oph.kouta.client

import org.scalatra.test.scalatest.ScalatraFlatSpec

import java.util.UUID

class HakemusPalveluClientSpec extends ScalatraFlatSpec {
  "Parsing active ids" should "return ids in list" in {
    val responseString =
      "{\"forms\": [{\"deleted\":null,\"key\":\"51666769-c664-40a8-893c-7bbe82399eea\",\"content\":[],\"name\":{\"fi\":\"Dummylomake - KOPIO\"}," +
        "\"created-by\":\"somebody\",\"locked\":null,\"id\":950454,\"created-time\":\"2022-08-16T13:53:36.708Z\",\"languages\":[\"fi\"]}," +
        "{\"deleted\":false,\"key\":\"837d728c-359e-4ad0-b61b-151bb790cb5b\",\"content\":[],\"name\":{\"fi\":\"Korkeakoulujen kevään 2022 toinen yhteishaku\"," +
        "\"sv\":\"Högskolornas andra gemensamma ansökan, våren 2022\",\"en\":\"Joint Application to Degree Programmes in Finnish/Swedish, Spring 2022\"}," +
        "\"created-by\":\"somebody\",\"locked\": false,\"id\":950453,\"created-time\":\"2022-03-10T11:53:38.532Z\",\"languages\":[\"fi\",\"sv\",\"en\"]}]}"
    HakemusPalveluClient.parseIds(responseString) should equal(
      Seq(
        "51666769-c664-40a8-893c-7bbe82399eea",
        "837d728c-359e-4ad0-b61b-151bb790cb5b"
      )
    )
  }

  it should "exclude inactive ids from list" in {
    val responseString =
      "{\"forms\": [{\"deleted\":null,\"key\":\"51666769-c664-40a8-893c-7bbe82399eea\",\"content\":[],\"name\":{\"fi\":\"Dummylomake - KOPIO\"}," +
        "\"created-by\":\"somebody\",\"locked\":null,\"id\":950454,\"created-time\":\"2022-08-16T13:53:36.708Z\",\"languages\":[\"fi\"]}," +
        "{\"deleted\":true,\"key\":\"837d728c-359e-4ad0-b61b-151bb790cb5b\",\"content\":[],\"name\":{\"fi\":\"Korkeakoulujen kevään 2022 toinen yhteishaku\"," +
        "\"sv\":\"Högskolornas andra gemensamma ansökan, våren 2022\",\"en\":\"Joint Application to Degree Programmes in Finnish/Swedish, Spring 2022\"}," +
        "\"created-by\":\"somebody\",\"locked\": false,\"id\":950453,\"created-time\":\"2022-03-10T11:53:38.532Z\",\"languages\":[\"fi\",\"sv\",\"en\"]}]}" +
        "{\"deleted\":null,\"key\":\"ad6329c3-79c5-4996-984e-d4b76d7eb129\",\"content\":[],\"name\":{\"fi\":\"Salpaus - jatkuva haku 2021\"},"+
        "\"created-by\":\"samrauni\",\"locked\":true,\"id\":771482,\"created-time\":\"2021-03-16T08:46:43.209Z\",\"languages\":[\"fi\"]}"
    HakemusPalveluClient.parseIds(responseString) should equal(
      Seq("51666769-c664-40a8-893c-7bbe82399eea")
    )
  }

  "Parsing empty response" should "return empty list" in {
    HakemusPalveluClient.parseIds("{\"forms\": []}") should equal(Seq())
  }
}
