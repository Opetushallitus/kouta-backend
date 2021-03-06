package fi.oph.kouta.service

import fi.oph.kouta.client.OidAndChildren
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.vm.sade.utils.slf4j.Logging

import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.UUID
import scala.util.Try

trait MigrationHelpers extends Logging {
  import org.json4s._
  private implicit val formats: DefaultFormats.type = DefaultFormats

  def isInteger(s: String): Boolean = { Try(s.toInt).isSuccess }
  def toDouble(s: String): Double = s.replace(',', '.').toDouble
  def isDouble(s: String): Boolean = { Try(toDouble(s)).isSuccess }
  def toOptionInt(i: Int): Option[Int] = if (i > 0) Some(i) else None

  def yearDoubleToYearAndMonth(s: String): Tuple2[Option[Int], Option[Int]] = {
    val d: Double = toDouble(s)
    val vuosi = d.toInt
    val kuukausi = (d % 1 * 12).toInt
    (toOptionInt(vuosi), toOptionInt(kuukausi))
  }

  def toKieli(kieli: String): Option[Kieli] = kieli match {
    case "kieli_fi" => Some(Fi)
    case "kieli_sv" => Some(Sv)
    case "kieli_en" => Some(En)
    case muu =>
      logger.warn(s"Tunnistamaton kieli $muu")
      None
  }
  def mapKieli(entry: (String,String)): Option[(Kieli, String)] = toKieli(entry._1).map(k => (k, entry._2))
  def toLocalDateTime(time : Long): LocalDateTime = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDateTime
  def toModified(time : Long): Modified = Modified(toLocalDateTime(time))

  import java.util.Calendar

  def toKieliMap(nimi: Map[String, String]): Map[Kieli, String] = {
    nimi.flatMap(mapKieli).filter(k => k._2.nonEmpty)
  }

  def joinKieliMaps(map1: Map[Kieli, String], map2: Map[Kieli, String], isHtml: Boolean = false): Map[Kieli, String] = {
    val allList = map1.toSeq ++ map2.toSeq
    allList.groupBy(_._1).mapValues(_.map(_._2).map(s => {
      if(isHtml) addP(s)
      else s
    }).mkString)
  }

  def addP(s: String): String = {
    if(s.nonEmpty && !s.trim.startsWith("<p>")) "<p>" + s + "</p>"
    else s
  }

  def localDateTimeToCalendar(localDateTime: LocalDateTime): Calendar = {
    val calendar = Calendar.getInstance
    calendar.clear()
    calendar.set(localDateTime.getYear, localDateTime.getMonthValue - 1, localDateTime.getDayOfMonth, localDateTime.getHour, localDateTime.getMinute, localDateTime.getSecond)
    calendar
  }
  
  def parseLisatiedot(result: JValue): Seq[Lisatieto] = {
    Seq(
      (result \ "kuvausKomo" \ "KOULUTUKSEN_RAKENNE" \ "tekstis").extractOpt[Map[String,String]].map(k => Lisatieto("koulutuksenlisatiedot_01#1", toKieliMap(k))), // Opintojen rakenne
      (result \ "kuvausKomo" \ "JATKOOPINTO_MAHDOLLISUUDET" \ "tekstis").extractOpt[Map[String,String]].map(k => Lisatieto("koulutuksenlisatiedot_02#1", toKieliMap(k))), // Jatko-opintomahdollisuudet
      (result \ "kuvausKomoto" \ "PAAAINEEN_VALINTA" \ "tekstis").extractOpt[Map[String,String]].map(k => Lisatieto("koulutuksenlisatiedot_03#1", toKieliMap(k))), // Suuntautumisvaihtoehdot
      (result \ "kuvausKomoto" \ "SIJOITTUMINEN_TYOELAMAAN" \ "tekstis").extractOpt[Map[String,String]].map(k => Lisatieto("koulutuksenlisatiedot_04#1", toKieliMap(k))), // Uramahdollisuudet
      (result \ "kuvausKomoto" \ "YHTEISTYO_MUIDEN_TOIMIJOIDEN_KANSSA" \ "tekstis").extractOpt[Map[String,String]].map(k => Lisatieto("koulutuksenlisatiedot_05#1", toKieliMap(k))), // Yhteistyö muiden toimijoiden kanssa
      (result \ "kuvausKomoto" \ "KANSAINVALISTYMINEN" \ "tekstis").extractOpt[Map[String,String]].map(k => Lisatieto("koulutuksenlisatiedot_06#1", toKieliMap(k))), // Kansainvälistyminen
      //(result \ "kuvausKomoto" \ "" \ "tekstis").extractOpt[Map[String,String]].map(k => Lisatieto("koulutuksenlisatiedot_07#1", toKieliMap(k))), // Opiskeluun liittyvät materiaalikulut
      //(result \ "kuvausKomoto" \ "" \ "tekstis").extractOpt[Map[String,String]].map(k => Lisatieto("koulutuksenlisatiedot_08#1", toKieliMap(k))), // Kohderyhmä
      (result \ "kuvausKomo" \ "PATEVYYS" \ "tekstis").extractOpt[Map[String,String]].map(k => Lisatieto("koulutuksenlisatiedot_09#1", toKieliMap(k))), // Koulutuksen antama pätevyys
      (result \ "kuvausKomoto" \ "TUTKIMUKSEN_PAINOPISTEET" \ "tekstis").extractOpt[Map[String,String]].map(k => Lisatieto("koulutuksenlisatiedot_10#1", toKieliMap(k))), // Tutkimuksen painopisteet
      (result \ "kuvausKomoto" \ "LOPPUKOEVAATIMUKSET" \ "tekstis").extractOpt[Map[String,String]].map(k => Lisatieto("koulutuksenlisatiedot_11#1", toKieliMap(k))) // Opinnäytetyö
    ).flatten.filter(l => l.teksti.nonEmpty)
  }

  def parseKuvaus(result: JValue): Map[Kieli, String] = {
    joinKieliMaps(
      (result \ "kuvausKomoto" \ "SISALTO" \ "tekstis").extractOpt[Map[String,String]].map(toKieliMap).getOrElse(Map()),
      (result \ "kuvausKomo" \ "TAVOITTEET" \ "tekstis").extractOpt[Map[String,String]].map(toKieliMap).getOrElse(Map()),
      isHtml = true
    )
  }

  def isYAMKKoulutus(koulutusasteUri: String): Boolean = {
    if(koulutusasteUri.contains("koulutusasteoph2002_71")) true
    else false
  }

  def toAjanjakso(a : Map[String, JValue]) = {
    val alkuPvm = toLocalDateTime(a("alkuPvm").extract[Long])
    val loppuPvm = a.get("loppuPvm").flatMap(_.extractOpt[Long]).map(toLocalDateTime)
    Ajanjakso(alkuPvm, loppuPvm)
  }
  def toJulkaisutila(tila: String): Julkaisutila = tila.toLowerCase() match {
    case _ => Tallennettu // Julkaisutila.withName(muu)
  }
  def toLiite(a: Map[String, JValue]): Option[Liite] =
    toKieli(a("kieliUri").extract[String]) match {
      case Some(kieli) =>
        Some(Liite(id = None,
          tyyppiKoodiUri = None,
          nimi = Map(kieli -> a("liitteenNimi").extract[String]),
          kuvaus = a("liitteenKuvaukset").extract[Map[String, String]].flatMap(mapKieli),
          toimitusaika = a("toimitettavaMennessa").extractOpt[Long].map(t => toLocalDateTime(t)),
          toimitustapa = Some(Lomake),
          toimitusosoite = None))
      case _ => None
    }


  def toKoulutuksenAlkamiskausi(result: JValue): Option[KoulutuksenAlkamiskausi] = {
    ((result \ "koulutuksenAlkamiskausi" \ "uri").extractOpt[String],
      (result \ "koulutuksenAlkamiskausi" \ "versio").extractOpt[String],
      (result \ "koulutuksenAlkamisvuosi").extractOpt[Int]) match {
      case (Some(uri), Some(versio), Some(vuosi)) => Some(KoulutuksenAlkamiskausi(
        alkamiskausityyppi = Some(AlkamiskausiJaVuosi),
        koulutuksenAlkamiskausiKoodiUri = Some(uri + "#" + versio),
        koulutuksenAlkamisvuosi = Some((vuosi + 1).toString)
      ))
      case _ => None
    }
  }

  def toOpetus(result: JValue, koulutustyyppi: Koulutustyyppi): Opetus = {
    val opetuskielis: Seq[String] = (result \ "opetuskielis" \ "uris").extract[Map[String, Int]].map {
      case ("kieli_fi", v) => s"oppilaitoksenopetuskieli_1#$v"
      case ("kieli_sv", v) => s"oppilaitoksenopetuskieli_2#$v"
      case ("kieli_en", v) => s"oppilaitoksenopetuskieli_4#$v"
    }.toSeq
    val onkoMaksullinen = (result \ "opintojenMaksullisuus").extractOpt[Boolean]
    val maksullisuustyyppi = if (onkoMaksullinen.contains(true)) Maksullinen else Maksuton
    val opetuskieletKuvaus: Kielistetty = (result \ "kuvausKomoto" \ "LISATIETOA_OPETUSKIELISTA" \ "tekstis").extractOpt[Map[String,String]].map(k => toKieliMap(k)).getOrElse(Map())
    val opetusaikaKoodiUrit: Seq[String] = (result \ "opetusAikas" \ "uris").extract[Map[String, Int]].map(k => s"${k._1}#${k._2}").toSeq
    val opetusaikaKuvaus: Kielistetty = Map()
    val opetustapaKoodiUrit: Seq[String] = (result \ "opetusPaikkas" \ "uris").extract[Map[String, Int]].map(k => s"${k._1}#${k._2}").toSeq
    val opetustapaKuvaus: Kielistetty = Map()
    val maksullisuusKuvaus: Kielistetty = Map()
    val maksunMaara: Option[Double] = None
    val koulutuksenAlkamiskausi: Option[KoulutuksenAlkamiskausi] = toKoulutuksenAlkamiskausi(result)
    val koulutusasteUri = (result \ "koulutusaste" \ "uri").extract[String]

    val lisatiedot: List[Lisatieto] =
      koulutustyyppi match {
        case Yo => List()
        case Amk if isYAMKKoulutus(koulutusasteUri) => List()
        case _ => parseLisatiedot(result).toList
      }

    val apuraha: Option[Apuraha] = None
    val onkoApuraha: Boolean = false

    val suunniteltuKesto: Tuple2[Option[Int], Option[Int]] = {
    ((result \ "suunniteltuKestoTyyppi" \ "uri").extractOpt[String], (result \ "suunniteltuKestoArvo").extractOpt[String]) match {
      case (Some(tyyppi), Some(arvo)) if tyyppi == "suunniteltukesto_01" && isInteger(arvo) => (Some(arvo.toInt), None)
      case (Some(tyyppi), Some(arvo)) if tyyppi == "suunniteltukesto_01" && isDouble(arvo) => yearDoubleToYearAndMonth(arvo)
      case (Some(tyyppi), Some(arvo)) if tyyppi == "suunniteltukesto_02" && isInteger(arvo) => (None, Some(arvo.toInt))
      case _ => (None, None)
    }
    }
    val suunniteltuKestoKuvaus: Kielistetty = Map()

    Opetus(
      opetuskieliKoodiUrit = opetuskielis,
      opetuskieletKuvaus = opetuskieletKuvaus,
      opetusaikaKoodiUrit = opetusaikaKoodiUrit,
      opetusaikaKuvaus = opetusaikaKuvaus,
      opetustapaKoodiUrit = opetustapaKoodiUrit,
      opetustapaKuvaus = opetustapaKuvaus,
      maksullisuustyyppi = Some(maksullisuustyyppi),
      maksullisuusKuvaus = maksullisuusKuvaus,
      maksunMaara = maksunMaara,
      koulutuksenAlkamiskausi = koulutuksenAlkamiskausi,
      lisatiedot = lisatiedot,
      onkoApuraha = onkoApuraha,
      apuraha = apuraha,
      suunniteltuKestoVuodet = suunniteltuKesto._1,
      suunniteltuKestoKuukaudet = suunniteltuKesto._2,
      suunniteltuKestoKuvaus = suunniteltuKestoKuvaus)
  }

}

object MigrationService extends MigrationService(OrganisaatioServiceImpl)

class MigrationService(organisaatioServiceImpl: OrganisaatioServiceImpl) extends MigrationHelpers {
  import org.json4s._
  private implicit val formats: DefaultFormats.type = DefaultFormats

  def f(r: JValue, q:String) = r.extract[Map[String, JValue]].filterKeys(_.toLowerCase.contains(q.toLowerCase))

  def resolveOrganisationOidForKoulutus(originalOid: OrganisaatioOid): OrganisaatioOid = {
    val originalOrganisation: OidAndChildren = organisaatioServiceImpl.getOrganisaatio(originalOid).get
    if(originalOrganisation.isKoulutustoimija || originalOrganisation.isOppilaitos) originalOrganisation.oid
    else organisaatioServiceImpl.findOppilaitosOidFromOrganisaationHierarkia(originalOid).get
  }

  def parseKoulutusFromResult(result: JValue, komo: JValue, koulutuskoodi2koulutusala: (String, Int) => Seq[String]): Koulutus = {
    val opetusTarjoajat = (result \ "opetusTarjoajat").extract[List[String]].map(oid => resolveOrganisationOidForKoulutus(OrganisaatioOid(oid))).distinct
    val opetuskielet = (result \ "opetuskielis" \ "meta").extract[Map[String, Any]].keys.map(toKieli)
    val hakukohteenNimet = toKieliMap((result \ "koulutusohjelma" \ "tekstis").extract[Map[String,String]])
    val nimi: Map[Kieli, String] =
      opetuskielet.flatten.map(kieli => (kieli, get(hakukohteenNimet,kieli))).toMap

    val tila = toJulkaisutila((result \ "tila").extract[String])
    val koulutusKoodiUri = s"${(result \ "koulutuskoodi" \ "uri").extract[String]}#${(result \ "koulutuskoodi" \ "versio").extract[Int]}"
    val koulutuksetKoodiUri = Seq(koulutusKoodiUri)
    val koulutusasteUri = (result \ "koulutusaste" \ "uri").extract[String]
    val koulutustyyppi: Koulutustyyppi = Koulutustyyppi.koulutusaste2koulutustyyppi.getOrElse(koulutusasteUri, Muu)
    val oid = KoulutusOid((komo \ "oid").extract[String])

    val opintojenLaajuusarvo = (result \ "opintojenLaajuusarvo" \ "uri").extractOpt[String]
    val opintojenLaajuusarvoVersio = (result \ "opintojenLaajuusarvo" \ "versio").extractOpt[Int]
    val tutikintonimikes = (result \ "tutkintonimikes" \ "uris").extract[Map[String,Int]].map {
      case (koodi,versio) => s"$koodi#$versio"
    }.toList
    val koulutusalaKoodiUrit =
      koulutuskoodi2koulutusala((result \ "koulutuskoodi" \ "uri").extract[String],
        (result \ "koulutuskoodi" \ "versio").extract[Int])
    val lisatiedot: Seq[Lisatieto] =
      koulutustyyppi match {
        case Yo => parseLisatiedot(result)
        case Amk if isYAMKKoulutus(koulutusasteUri) => parseLisatiedot(result)
        case _ => Seq()
      }
    val kuvaus: Map[Kieli, String] = parseKuvaus(result)
    val metadata: KoulutusMetadata =
      koulutustyyppi match {
        case Amk => AmmattikorkeakouluKoulutusMetadata(
          kuvaus = if(isYAMKKoulutus(koulutusasteUri)) kuvaus else Map(),
          lisatiedot = lisatiedot,
          koulutusalaKoodiUrit = koulutusalaKoodiUrit,
          tutkintonimikeKoodiUrit = tutikintonimikes,
          opintojenLaajuusKoodiUri = opintojenLaajuusarvo.map(arvo => s"$arvo#${opintojenLaajuusarvoVersio.get}"),
          kuvauksenNimi = opetuskielet.flatten.map(k => k -> "").toMap)
        case Yo => YliopistoKoulutusMetadata(
          kuvaus = kuvaus,
          lisatiedot = lisatiedot,
          koulutusalaKoodiUrit = koulutusalaKoodiUrit,
          tutkintonimikeKoodiUrit = tutikintonimikes,
          opintojenLaajuusKoodiUri = opintojenLaajuusarvo.map(arvo => s"$arvo#${opintojenLaajuusarvoVersio.get}"),
          kuvauksenNimi = opetuskielet.flatten.map(k => k -> "").toMap
        )
        case _ => throw new RuntimeException(s"Tuntematon koulutustyyppi $koulutusasteUri koulutuksella $oid")
      }

    Koulutus(oid = Some(oid),
    johtaaTutkintoon = (result \ "johtaaTutkintoon").extract[Boolean],
    tila = tila,
    kielivalinta = opetuskielet.flatten.toSeq,
    tarjoajat = opetusTarjoajat,
    koulutuksetKoodiUri = koulutuksetKoodiUri,
    nimi = nimi,
    koulutustyyppi = Koulutustyyppi.koulutusaste2koulutustyyppi.getOrElse(koulutusasteUri, Muu),
    metadata = Some(metadata),
    julkinen = false,
    esikatselu = false,
    muokkaaja = UserOid((result \ "modifiedBy").extract[String]),
    organisaatioOid = resolveOrganisationOidForKoulutus(OrganisaatioOid((result \ "organisaatio" \ "oid").extract[String])),
    teemakuva = None,
    ePerusteId = None,
    modified = (result \ "modified").extractOpt[Long].map(toModified))
  }

  def parseToteutusFromResult(result: JValue): Toteutus = {
    val opetusTarjoajat = (result \ "opetusTarjoajat").extract[List[String]].map(OrganisaatioOid)

    val opetuskielet = (result \ "opetuskielis" \ "meta").extract[Map[String, Any]].keys.map(toKieli)
    val hakukohteenNimet = toKieliMap((result \ "koulutusohjelma" \ "tekstis").extract[Map[String,String]])
    val nimet: Map[Kieli, String] =
      opetuskielet.flatten.map(kieli => (kieli, get(hakukohteenNimet,kieli))).toMap
    val kuvaus: Map[Kieli, String] = parseKuvaus(result)
    val koulutusasteUri = (result \ "koulutusaste" \ "uri").extract[String]
    val oid = (result \ "oid").extractOpt[String].map(ToteutusOid)
    val koulutustyyppi: Koulutustyyppi = Koulutustyyppi.koulutusaste2koulutustyyppi.getOrElse(koulutusasteUri, Muu)
    val metadata: Option[ToteutusMetadata] =
      Some(koulutustyyppi match {
        case Amk => AmmattikorkeakouluToteutusMetadata(
          kuvaus = if(isYAMKKoulutus(koulutusasteUri)) Map() else kuvaus,
          opetus = Some(toOpetus(result, koulutustyyppi)),
          asiasanat = List(),
          ammattinimikkeet = List(),
          yhteyshenkilot = Seq(),
          alemmanKorkeakoulututkinnonOsaamisalat = Seq(),
          ylemmanKorkeakoulututkinnonOsaamisalat = Seq()
        )
        case Yo => YliopistoToteutusMetadata(
          kuvaus = Map(),
          opetus = Some(toOpetus(result, koulutustyyppi)),
          asiasanat = List(),
          ammattinimikkeet = List(),
          yhteyshenkilot = Seq(),
          alemmanKorkeakoulututkinnonOsaamisalat = Seq(),
          ylemmanKorkeakoulututkinnonOsaamisalat = Seq()
        )
        case _ => throw new RuntimeException(s"Tuntematon koulutustyyppi $koulutusasteUri toteutuksella $oid")
      })

    Toteutus(
      oid = oid,
      koulutusOid = KoulutusOid((result \ "komoOid").extract[String]),
      tila = toJulkaisutila((result \ "tila").extract[String]),
      tarjoajat = opetusTarjoajat,
      nimi = nimet,
      metadata = metadata,
      sorakuvausId = None,
      muokkaaja = UserOid((result \ "modifiedBy").extract[String]),
      organisaatioOid = OrganisaatioOid((result \ "organisaatio" \ "oid").extract[String]),
      kielivalinta = opetuskielet.flatten.toSeq,
      teemakuva = None,
      modified = (result \ "modified").extractOpt[Long].map(toModified))
  }

  def get(kk: Map[Kieli, String], k: Kieli): String = Seq(k, Fi, Sv, En).flatMap(kk.get).head

  def parseValintakokeetFromResult(result: JValue): Map[String, Valintakoe] = {
    def toValintakoe(obj: JObject): Option[(String, Valintakoe)] = {
      def toValintakoetilaisuus(obj: JValue, kieli: Kieli): Valintakoetilaisuus = {
        def toOsoite(obj: JValue, kieli: Kieli): Option[Osoite] = {
          val katuosoite = (obj \ "osoiterivi1").extractOpt[String]
          val postinumeroUri = (obj \ "postinumero").extractOpt[String].map(_ + "#2") // TODO: Viimeisimmän koodiversion selvittäminen koodistopalvelusta
          Some(Osoite(
            osoite = if(katuosoite.isDefined) Map(kieli -> katuosoite.get) else Map(),
            postinumeroKoodiUri = postinumeroUri
          ))
        }
        val osoite = toOsoite(obj \ "osoite", kieli)
        val aika = ((obj \ "alkaa").extractOpt[Long], (obj \ "loppuu").extractOpt[Long]) match {
          case (Some(alkaa), Some(loppuu)) if alkaa > 0 && loppuu > 0 && alkaa < loppuu => {
            Some(Ajanjakso(
              alkaa = toLocalDateTime(alkaa),
              paattyy = Some(toLocalDateTime(loppuu))
            ))
          }
          case _ => None
        }
        val lisatietoja = (obj \ "lisatiedot").extractOpt[String]
        Valintakoetilaisuus(
          osoite = osoite,
          aika = aika,
          jarjestamispaikka = Map(),
          lisatietoja = if(lisatietoja.isDefined) Map(kieli -> lisatietoja.get) else Map()
        )
      }
      toKieli((obj \ "kieliUri").extract[String]).map(kieli => {
        val oid = (obj \ "oid").extract[String]
        val nimi = (obj \ "valintakoeNimi").extract[String]
        val valintakokeenKuvaus = (obj \ "valintakokeenKuvaus" \ "teksti").extractOpt[String]
        val tilaisuudet = (obj \ "valintakoeAjankohtas").extract[List[JValue]].map(toValintakoetilaisuus(_, kieli))
        (oid, Valintakoe(id = None,
          tyyppiKoodiUri = Some("valintakokeentyyppi_8#1"),
          nimi = Map(kieli -> nimi),
          metadata = Some(ValintakoeMetadata(
            tietoja = if(valintakokeenKuvaus.isDefined) Map(kieli -> valintakokeenKuvaus.get) else Map()
          )),
          tilaisuudet = tilaisuudet))
      })
    }
    (result \ "valintakokeet").extract[List[JObject]].flatMap(toValintakoe).toMap
  }

  def parseHakukohdeFromResult(result: JValue): Hakukohde = {
    val tarjoajaOids = (result \ "tarjoajaOids").extract[List[String]]
    val opetuskielet = (result \ "opetusKielet").extract[List[String]].map(toKieli)
    val toteutusOid = ToteutusOid((result \ "hakukohdeKoulutusOids").extract[List[String]].head)
    val hakulomakeAtaruId = (result \ "ataruLomakeAvain").extractOpt[String].map(UUID.fromString)
    val hakukohteenNimet = toKieliMap((result \ "hakukohteenNimet").extract[Map[String, String]])
    val nimi: Map[Kieli, String] =
      opetuskielet.flatten.map(kieli => (kieli, get(hakukohteenNimet,kieli))).toMap

    val liitteet = (result \ "hakukohteenLiitteet").extract[List[Map[String, JValue]]].flatMap(toLiite)

    val pohjakoulutusvaatimus = (result \ "hakukelpoisuusvaatimusUris").extract[List[String]]
      .map(_.split("_").last).map(versio => s"pohjakoulutusvaatimuskouta_$versio#1")

    val aloituspaikat = Aloituspaikat(lukumaara = (result \ "aloituspaikatLkm").extractOpt[Int], ensikertalaisille = (result \ "ensikertalaistenAloituspaikat").extractOpt[Int])

    Hakukohde(oid = Some(HakukohdeOid((result \ "oid").extract[String])),
      toteutusOid = toteutusOid,
      hakuOid = HakuOid((result \ "hakuOid").extract[String]),
      tila = toJulkaisutila((result \ "tila").extract[String]),
      nimi = nimi,
      jarjestyspaikkaOid = Some(OrganisaatioOid(tarjoajaOids.head)),
      hakulomaketyyppi = hakulomakeAtaruId match {
        case Some(_) => Some(Ataru)
        case None => None
      },
      hakulomakeAtaruId = hakulomakeAtaruId,
      hakulomakeKuvaus = Map(),
      hakulomakeLinkki = Map(),
      kaytetaanHaunHakulomaketta = Some(true), // TODO
      hakuajat = Seq(), // TODO TODO TODO
      metadata = Some(HakukohdeMetadata( //TODO: Suurin osa hakukohteen kentistä pitäisi siirtää metadatan sisään!
        koulutuksenAlkamiskausi = None,
        kaytetaanHaunAlkamiskautta = Some(true),
        aloituspaikat = Some(aloituspaikat))),
      muokkaaja = UserOid((result \ "modifiedBy").extract[String]),
      organisaatioOid = OrganisaatioOid(tarjoajaOids.head),
      kielivalinta = opetuskielet.flatten,
      modified = (result \ "modified").extractOpt[Long].map(toModified),
      pohjakoulutusvaatimusKoodiUrit = pohjakoulutusvaatimus,
      pohjakoulutusvaatimusTarkenne = Map(), // TODO
      muuPohjakoulutusvaatimus = Map(), // TODO
      liitteet = liitteet,
      toinenAsteOnkoKaksoistutkinto = (result \ "kaksoisTutkinto").extractOpt[Boolean],
      kaytetaanHaunAikataulua = Some(true), // TODO
      valintaperusteId = None,
      liitteetOnkoSamaToimitusaika = None,
      liitteetOnkoSamaToimitusosoite = None,
      liitteidenToimitusaika = None,
      liitteidenToimitustapa = None,
      liitteidenToimitusosoite = None,
      valintakokeet = Seq())
  }

  def parseHakuFromResult(result: JValue): Haku = {
    val tarjoajaOids = (result \ "tarjoajaOids").extract[List[String]]
    val nimi: Kielistetty = (result \ "nimi").extract[Map[String,String]].flatMap(mapKieli)
    val hakulomakeAtaruId = (result \ "ataruLomakeAvain").extractOpt[String].map(UUID.fromString)
    val yhteyshenkilot: Seq[Yhteyshenkilo] = Seq()
    val tulevaisuudenAikataulu: Seq[Ajanjakso] = Seq()
    val koulutuksenAlkamiskausi: Option[KoulutuksenAlkamiskausi] =
      Some(KoulutuksenAlkamiskausi(alkamiskausityyppi = Some(AlkamiskausiJaVuosi),
        henkilokohtaisenSuunnitelmanLisatiedot = Map.empty,
        koulutuksenAlkamispaivamaara = None,
        koulutuksenPaattymispaivamaara = None,
        koulutuksenAlkamiskausiKoodiUri = (result \ "koulutuksenAlkamiskausiUri").extractOpt[String],
        koulutuksenAlkamisvuosi = (result \ "koulutuksenAlkamisVuosi").extractOpt[Int].map(vuosi => (vuosi + 1).toString)))
    val hakuajat = (result \ "hakuaikas").extract[List[Map[String, JValue]]].map(toAjanjakso)

    Haku(
      oid = Some(HakuOid((result \ "oid").extract[String])),
      tila = toJulkaisutila((result \ "tila").extract[String]),
      nimi = nimi,
      hakutapaKoodiUri = (result \ "hakutapaUri").extractOpt[String],
      hakukohteenLiittamisenTakaraja = None,
      hakukohteenMuokkaamisenTakaraja = None,
      ajastettuJulkaisu = None,
      kohdejoukkoKoodiUri = (result \ "kohdejoukkoUri").extractOpt[String],
      kohdejoukonTarkenneKoodiUri = None,
      hakulomakeAtaruId = hakulomakeAtaruId,
      hakulomaketyyppi = hakulomakeAtaruId match {
        case Some(_) => Some(Ataru)
        case None => None
      },
      hakulomakeKuvaus = Map(),
      hakulomakeLinkki = Map(),
      metadata = Some(HakuMetadata(
        yhteyshenkilot = yhteyshenkilot,
        tulevaisuudenAikataulu = tulevaisuudenAikataulu,
        koulutuksenAlkamiskausi = koulutuksenAlkamiskausi)),
      organisaatioOid = OrganisaatioOid(tarjoajaOids.head),
      hakuajat = hakuajat,
      muokkaaja = UserOid((result \ "modifiedBy").extract[String]),
      kielivalinta = nimi.keySet.toSeq,
      modified = (result \ "modified").extractOpt[Long].map(toModified))
  }
}
