package fi.oph.kouta.service

import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.{UUID}

import fi.oph.kouta.client.{CallerId, HttpClient}
import fi.oph.kouta.domain.{Ajanjakso, AlkamiskausiJaVuosi, Amk, AmmattikorkeakouluKoulutusMetadata, AmmattikorkeakouluToteutusMetadata, Arkistoitu, Ataru, En, Fi, Haku, HakuMetadata, Hakukohde, Julkaisutila, Kieli, Kielistetty, KoulutuksenAlkamiskausi, Koulutus, KoulutusMetadata, Koulutustyyppi, Liite, Lisatieto, Lomake, Muu, Opetus, Sv, Tallennettu, Toteutus, ToteutusMetadata, Yhteyshenkilo, YliopistoKoulutusMetadata, YliopistoToteutusMetadata, Yo}
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid, KoulutusOid, OrganisaatioOid, ToteutusOid, UserOid}

trait MigrationHelpers {
  import org.json4s._
  private implicit val formats: DefaultFormats.type = DefaultFormats

  def toKieli(kieli: String): Kieli = kieli match {
    case "kieli_fi" => Fi
    case "kieli_sv" => Sv
    case "kieli_en" => En
    case _ => throw new RuntimeException(s"Tunnistamaton kieli $kieli")
  }
  def mapKieli(entry: (String,String)): (Kieli, String) = (toKieli(entry._1), entry._2)
  def toLocalDateTime(time : Long): LocalDateTime = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDateTime

  import java.util.Calendar

  def toKieliMap(nimi: Map[String, String]): Map[Kieli, String] = {
    nimi.map(mapKieli).filter(k => !k._2.isEmpty)
  }

  def localDateTimeToCalendar(localDateTime: LocalDateTime): Calendar = {
    val calendar = Calendar.getInstance
    calendar.clear()
    calendar.set(localDateTime.getYear, localDateTime.getMonthValue - 1, localDateTime.getDayOfMonth, localDateTime.getHour, localDateTime.getMinute, localDateTime.getSecond)
    calendar
  }
  def setYearAfter(source: LocalDateTime, target: LocalDateTime): LocalDateTime = {
    val nowCalendar = localDateTimeToCalendar(target)
    val calendar = localDateTimeToCalendar(source)
    if(nowCalendar.before(calendar)) {
      source
    } else {
      calendar.set(Calendar.YEAR, source.getYear + 1)
      setYearAfter(LocalDateTime.ofInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId()), target)
    }
  }

  def ensureLoppuInFuture(alku: LocalDateTime, loppu: Option[LocalDateTime]): (LocalDateTime, Option[LocalDateTime]) = {
    loppu match {
      case None =>
        (alku, loppu)
      case Some(loppu) =>
        val now = LocalDateTime.now()
        if(now.isBefore(loppu)) {
          (alku, Some(loppu))
        } else {
          (setYearAfter(alku, now), Some(setYearAfter(loppu, now)))
        }
    }
  }

  def toAjanjakso(a : Map[String, JValue]) = {
    val calendar = Calendar.getInstance()
    val alkuPvm = toLocalDateTime(a("alkuPvm").extract[Long])
    val loppuPvm = a.get("loppuPvm").flatMap(_.extractOpt[Long]).map(toLocalDateTime)
    val (alku, loppu) = ensureLoppuInFuture(alkuPvm,loppuPvm)


    Ajanjakso(alku,loppu)
  }
  def toJulkaisutila(tila: String): Julkaisutila = tila.toLowerCase() match {
    case "peruttu" => Arkistoitu
    case "kopioitu" => Tallennettu
    case "luonnos" => Tallennettu
    case _ => Tallennettu // Julkaisutila.withName(muu)
  }
  def toLiite(a: Map[String, JValue]): Liite =
    Liite(id = None,
      tyyppiKoodiUri = None,
      nimi = Map(toKieli(a("kieliUri").extract[String]) -> a("liitteenNimi").extract[String]),
      kuvaus = a("liitteenKuvaukset").extract[Map[String, String]].map(mapKieli),
      toimitusaika = (a("toimitettavaMennessa").extractOpt[Long]).map(t => setYearAfter(toLocalDateTime(t),LocalDateTime.now())),
      toimitustapa = Some(Lomake),
      toimitusosoite = None)

  def toOpetus(result: JValue): Opetus = {
    val opetuskielis: Seq[String] = (result \ "opetuskielis" \ "uris").extract[Map[String, Int]].map {
      case ("kieli_fi", v) => s"oppilaitoksenopetuskieli_1#$v"
      case ("kieli_sv", v) => s"oppilaitoksenopetuskieli_2#$v"
      case ("kieli_en", v) => s"oppilaitoksenopetuskieli_4#$v"
    }.toSeq
    val onkoMaksullinen = (result \ "opintojenMaksullisuus").extractOpt[Boolean]
    val opetuskieletKuvaus: Kielistetty = Map()
    val opetusaikaKoodiUrit: Seq[String] = (result \ "opetusAikas" \ "uris").extract[Map[String, Int]].map(k => s"${k._1}#${k._2}").toSeq
    val opetusaikaKuvaus: Kielistetty = Map()
    val opetustapaKoodiUrit: Seq[String] = (result \ "opetusPaikkas" \ "uris").extract[Map[String, Int]].map(k => s"${k._1}#${k._2}").toSeq
    val opetustapaKuvaus: Kielistetty = Map()
    val maksullisuusKuvaus: Kielistetty = Map()
    val maksunMaara: Option[Double] = None
    val koulutuksenAlkamiskausiUUSI: Option[KoulutuksenAlkamiskausi] = None
    val koulutuksenTarkkaAlkamisaika: Option[Boolean] = None
    val koulutuksenAlkamispaivamaara: Option[LocalDateTime] = None
    val koulutuksenPaattymispaivamaara: Option[LocalDateTime] = None
    val koulutuksenAlkamiskausi: Option[String] = None
    val koulutuksenAlkamisvuosi: Option[Int] = None
    val lisatiedot: Seq[Lisatieto] = Seq()
    val onkoStipendia: Option[Boolean] = Some(false)
    val stipendinMaara: Option[Double] = None
    val stipendinKuvaus: Kielistetty = Map()
    val suunniteltuKestoVuodet: Option[Int] = None
    val suunniteltuKestoKuukaudet: Option [Int] = None
    val suunniteltuKestoKuvaus: Kielistetty = Map()

    Opetus(
      opetuskieliKoodiUrit = opetuskielis,
      opetuskieletKuvaus = opetuskieletKuvaus,
      opetusaikaKoodiUrit = opetusaikaKoodiUrit,
      opetusaikaKuvaus = opetusaikaKuvaus,
      opetustapaKoodiUrit = opetustapaKoodiUrit,
      opetustapaKuvaus = opetustapaKuvaus,
      onkoMaksullinen = onkoMaksullinen,
      maksullisuusKuvaus = maksullisuusKuvaus,
      maksunMaara = maksunMaara,
      koulutuksenAlkamiskausiUUSI = koulutuksenAlkamiskausiUUSI,
      koulutuksenTarkkaAlkamisaika = koulutuksenTarkkaAlkamisaika,
      koulutuksenAlkamispaivamaara = koulutuksenAlkamispaivamaara,
      koulutuksenPaattymispaivamaara = koulutuksenPaattymispaivamaara,
      koulutuksenAlkamiskausi = koulutuksenAlkamiskausi,
      koulutuksenAlkamisvuosi = koulutuksenAlkamisvuosi,
      lisatiedot = lisatiedot,
      onkoStipendia = onkoStipendia,
      stipendinMaara = stipendinMaara,
      stipendinKuvaus = stipendinKuvaus,
      suunniteltuKestoVuodet = suunniteltuKestoVuodet,
      suunniteltuKestoKuukaudet = suunniteltuKestoKuukaudet,
      suunniteltuKestoKuvaus = suunniteltuKestoKuvaus)
  }

}

class MigrationService extends MigrationHelpers {
  import org.json4s._
  private implicit val formats: DefaultFormats.type = DefaultFormats

  def f(r: JValue, q:String) = r.extract[Map[String, JValue]].filterKeys(_.toLowerCase.contains(q.toLowerCase))

  def parseKoulutusFromResult(result: JValue, komo: JValue): Koulutus = {
    val opetusTarjoajat = (result \ "opetusTarjoajat").extract[List[String]].map(OrganisaatioOid)
    val opetuskielet = (result \ "opetuskielis" \ "meta").extract[Map[String, Any]].keys.map(toKieli)
    val hakukohteenNimet = toKieliMap((result \ "koulutusohjelma" \ "tekstis").extract[Map[String,String]])
    val nimi: Map[Kieli, String] =
      opetuskielet.map(kieli => (kieli, get(hakukohteenNimet,kieli))).toMap

    val tila = toJulkaisutila((result \ "tila").extract[String])
    val koulutusKoodiUri = s"${(result \ "koulutuskoodi" \ "uri").extract[String]}#${(result \ "koulutuskoodi" \ "versio").extract[Int]}"
    val koulutusasteUri = (result \ "koulutusaste" \ "uri").extract[String]
    val koulutustyyppi: Koulutustyyppi = Koulutustyyppi.koulutusaste2koulutustyyppi.getOrElse(koulutusasteUri, Muu)
    val oid = KoulutusOid((komo \ "oid").extract[String])
    //val kuvausNimi = toKieliMap((result \ "kuvausKomo" \ "TAVOITTEET" \ "tekstis").extract[Map[String,String]])

    val opintojenLaajuusarvo = (result \ "opintojenLaajuusarvo" \ "uri").extractOpt[String]
    val opintojenLaajuusarvoVersio = (result \ "opintojenLaajuusarvo" \ "versio").extractOpt[Int]
    val tutikintonimikes = (result \ "tutkintonimikes" \ "uris").extract[Map[String,Int]].map {
      case (koodi,versio) => s"$koodi#$versio"
    }.toList
    val lisatiedot: Seq[Lisatieto] = Seq(
      Lisatieto("koulutuksenlisatiedot_01#1", toKieliMap((result \ "kuvausKomo" \ "KOULUTUKSEN_RAKENNE" \ "tekstis").extract[Map[String,String]])),
      Lisatieto("koulutuksenlisatiedot_02#1", toKieliMap((result \ "kuvausKomo" \ "JATKOOPINTO_MAHDOLLISUUDET" \ "tekstis").extract[Map[String,String]])),
      Lisatieto("koulutuksenlisatiedot_03#1", toKieliMap((result \ "kuvausKomoto" \ "SIJOITTUMINEN_TYOELAMAAN" \ "tekstis").extract[Map[String,String]])),
      Lisatieto("koulutuksenlisatiedot_04#1", toKieliMap((result \ "kuvausKomoto" \ "YHTEISTYO_MUIDEN_TOIMIJOIDEN_KANSSA" \ "tekstis").extract[Map[String,String]])),
      Lisatieto("koulutuksenlisatiedot_06#1", toKieliMap((result \ "kuvausKomoto" \ "KANSAINVALISTYMINEN" \ "tekstis").extract[Map[String,String]])),
      Lisatieto("koulutuksenlisatiedot_10#1", toKieliMap((result \ "kuvausKomoto" \ "SISALTO" \ "tekstis").extract[Map[String,String]]))
    )
    val metadata: KoulutusMetadata =
      koulutustyyppi match {
        case Amk => AmmattikorkeakouluKoulutusMetadata(
          kuvaus = Map(),
          lisatiedot = lisatiedot,
          koulutusalaKoodiUrit = Seq(),
          tutkintonimikeKoodiUrit = tutikintonimikes,
          opintojenLaajuusKoodiUri = opintojenLaajuusarvo.map(arvo => s"$arvo#${opintojenLaajuusarvoVersio.get}"),
          kuvauksenNimi = opetuskielet.map(k => k -> "").toMap)
        case Yo => YliopistoKoulutusMetadata(
          kuvaus = Map(),
          lisatiedot = lisatiedot,
          koulutusalaKoodiUrit = Seq(),
          tutkintonimikeKoodiUrit = Seq(),
          opintojenLaajuusKoodiUri = None,
          kuvauksenNimi = opetuskielet.map(k => k -> "").toMap
        )
        case _ => throw new RuntimeException(s"Tuntematon koulutustyyppi $koulutusasteUri koulutuksella $oid")
      }

    Koulutus(oid = Some(oid),
    johtaaTutkintoon = (result \ "johtaaTutkintoon").extract[Boolean],
    tila = tila,
    kielivalinta = opetuskielet.toSeq,
    tarjoajat = opetusTarjoajat,
    koulutusKoodiUri = Some(koulutusKoodiUri),
    nimi = nimi,
    koulutustyyppi = Koulutustyyppi.koulutusaste2koulutustyyppi.getOrElse(koulutusasteUri, Muu),
    metadata = Some(metadata),
    julkinen = false,
    esikatselu = false,
    muokkaaja = UserOid((result \ "modifiedBy").extract[String]),
    organisaatioOid = OrganisaatioOid((result \ "organisaatio" \ "oid").extract[String]),
    teemakuva = None,
    ePerusteId = None,
    modified = (result \ "modified").extractOpt[Long].map(toLocalDateTime))
  }

  def parseToteutusFromResult(result: JValue): Toteutus = {
    val opetusTarjoajat = (result \ "opetusTarjoajat").extract[List[String]].map(OrganisaatioOid)

    val opetuskielet = (result \ "opetuskielis" \ "meta").extract[Map[String, Any]].keys.map(toKieli)
    val hakukohteenNimet = toKieliMap((result \ "koulutusohjelma" \ "tekstis").extract[Map[String,String]])
    val nimet: Map[Kieli, String] =
      opetuskielet.map(kieli => (kieli, get(hakukohteenNimet,kieli))).toMap
    val kuvaus = toKieliMap((result \ "kuvausKomo" \ "TAVOITTEET" \ "tekstis").extract[Map[String,String]])
    val koulutusasteUri = (result \ "koulutusaste" \ "uri").extract[String]
    val oid = (result \ "oid").extractOpt[String].map(ToteutusOid)
    val koulutustyyppi: Koulutustyyppi = Koulutustyyppi.koulutusaste2koulutustyyppi.getOrElse(koulutusasteUri, Muu)
    val metadata: Option[ToteutusMetadata] =
      Some(koulutustyyppi match {
        case Amk => AmmattikorkeakouluToteutusMetadata(
          kuvaus = kuvaus,
          opetus = Some(toOpetus(result)),
          asiasanat = List(),
          ammattinimikkeet = List(),
          yhteyshenkilot = Seq(),
          alemmanKorkeakoulututkinnonOsaamisalat = Seq(),
          ylemmanKorkeakoulututkinnonOsaamisalat = Seq()
        )
        case Yo => YliopistoToteutusMetadata(
          kuvaus = Map(),
          opetus = Some(toOpetus(result)),
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
      kielivalinta = opetuskielet.toSeq,
      teemakuva = None,
      modified = (result \ "modified").extractOpt[Long].map(toLocalDateTime))
  }

  def get(kk: Map[Kieli, String], k: Kieli): String = Seq(k, Fi, Sv, En).flatMap(kk.get).head

  def parseHakukohdeFromResult(result: JValue): Hakukohde = {
    val tarjoajaOids = (result \ "tarjoajaOids").extract[List[String]]
    val opetuskielet = (result \ "opetusKielet").extract[List[String]].map(toKieli)
    val toteutusOid = ToteutusOid((result \ "hakukohdeKoulutusOids").extract[List[String]].head)
    val hakulomakeAtaruId = (result \ "ataruLomakeAvain").extractOpt[String].map(UUID.fromString)
    val hakukohteenNimet = toKieliMap((result \ "hakukohteenNimet").extract[Map[String, String]])
    val nimi: Map[Kieli, String] =
      opetuskielet.map(kieli => (kieli, get(hakukohteenNimet,kieli))).toMap

    val liitteet = (result \ "hakukohteenLiitteet").extract[List[Map[String, JValue]]].map(toLiite)

    val pohjakoulutusvaatimus = ((result \ "hakukelpoisuusvaatimusUris").extract[List[String]])
      .map(_.split("_").last).map(versio => s"pohjakoulutusvaatimuskouta_$versio#1")

    Hakukohde(oid = Some(HakukohdeOid((result \ "oid").extract[String])),
      toteutusOid = toteutusOid,
      hakuOid = HakuOid((result \ "hakuOid").extract[String]),
      tila = toJulkaisutila((result \ "tila").extract[String]),
      nimi = nimi,
      alkamiskausiKoodiUri = None,
      alkamisvuosi = None,
      kaytetaanHaunAlkamiskautta = Some(true), // TODO
      jarjestyspaikkaOid = None,
      hakulomaketyyppi = hakulomakeAtaruId match {
        case Some(_) => Some(Ataru)
        case None => None
      },
      hakulomakeAtaruId = hakulomakeAtaruId,
      hakulomakeKuvaus = Map(),
      hakulomakeLinkki = Map(),
      kaytetaanHaunHakulomaketta = Some(true), // TODO
      aloituspaikat = (result \ "aloituspaikatLkm").extractOpt[Int],
      ensikertalaisenAloituspaikat = (result \ "ensikertalaistenAloituspaikat").extractOpt[Int],
      hakuajat = Seq(), // TODO TODO TODO
      metadata = None, //TODO: Suurin osa hakukohteen kentistä pitäisi siirtää metadatan sisään!
      muokkaaja = UserOid((result \ "modifiedBy").extract[String]),
      organisaatioOid = OrganisaatioOid(tarjoajaOids.head),
      kielivalinta = opetuskielet,
      modified = (result \ "modified").extractOpt[Long].map(toLocalDateTime),
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
    val nimi: Kielistetty = (result \ "nimi").extract[Map[String,String]].map(mapKieli)
    val hakulomakeAtaruId = (result \ "ataruLomakeAvain").extractOpt[String].map(UUID.fromString)
    val yhteyshenkilot: Seq[Yhteyshenkilo] = Seq()
    val tulevaisuudenAikataulu: Seq[Ajanjakso] = Seq()
    val koulutuksenAlkamiskausi: Option[KoulutuksenAlkamiskausi] =
      Some(KoulutuksenAlkamiskausi(alkamiskausityyppi = Some(AlkamiskausiJaVuosi),
        henkilokohtaisenSuunnitelmanLisatiedot = Map.empty,
        koulutuksenAlkamispaivamaara = None,
        koulutuksenPaattymispaivamaara = None,
        koulutuksenAlkamiskausiKoodiUri = (result \ "koulutuksenAlkamiskausiUri").extractOpt[String],
        koulutuksenAlkamisvuosi = (result \ "koulutuksenAlkamisVuosi").extractOpt[String]))

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
      modified = (result \ "modified").extractOpt[Long].map(toLocalDateTime))

  }

}
