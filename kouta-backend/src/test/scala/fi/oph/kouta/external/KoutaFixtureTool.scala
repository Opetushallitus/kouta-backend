package fi.oph.kouta.external

import java.time.temporal.{ChronoUnit, TemporalField}
import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util.UUID
import fi.oph.kouta.TestData
import fi.oph.kouta.TestDataGenerator.hakukohde
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.util.KoutaJsonFormats
import fi.oph.kouta.validation.{NoErrors, Validatable}
import org.json4s.jackson.Serialization.{read, write}

import scala.collection.JavaConverters._

object KoutaFixtureTool extends KoutaJsonFormats {

  private var koulutukset:Map[String, Map[String, String]] = Map()
  private var toteutukset:Map[String, Map[String, String]] = Map()
  private var haut:Map[String, Map[String, String]] = Map()
  private var hakukohteet:Map[String, Map[String, String]] = Map()
  private var valintaperusteet:Map[String, Map[String, String]] = Map()
  private var sorakuvaukset:Map[String, Map[String, String]] = Map()
  private var oppilaitokset:Map[String, Map[String, String]] = Map()
  private var oppilaitostenOsat:Map[String, Map[String, String]] = Map()

  def addKoulutus(oid: String, jParams: java.util.Map[String, String]): Unit =
    addKoulutus(oid, jParams.asScala.toMap)

  def addKoulutus(oid: String, params: Map[String, String]): Unit =
    koulutukset += (oid -> params)

  def updateKoulutus(oid: String, jParams: java.util.Map[String, String]): Unit =
    koulutukset += (oid -> (koulutukset.getOrElse(oid, Map()) ++ jParams.asScala.toMap))

  def addToteutus(oid: String, jParams: java.util.Map[String, String]): Unit =
    addToteutus(oid, jParams.asScala.toMap)

  def addToteutus(oid: String, params: Map[String, String]): Unit =
    toteutukset += (oid -> params)

  def updateToteutus(oid: String, jParams: java.util.Map[String, String]): Unit =
    toteutukset += (oid -> (toteutukset.getOrElse(oid, Map()) ++ jParams.asScala.toMap))

  def addHaku(oid: String, jParams: java.util.Map[String, String]): Unit =
    addHaku(oid, jParams.asScala.toMap)

  def addHaku(oid: String, params: Map[String, String]): Unit =
    haut += (oid -> params)

  def updateHaku(oid: String, jParams: java.util.Map[String, String]): Unit =
    haut += (oid -> (haut.getOrElse(oid, Map()) ++ jParams.asScala.toMap))

  def addHakukohde(oid: String, jParams: java.util.Map[String, String]): Unit =
    addHakukohde(oid, jParams.asScala.toMap)

  def addHakukohde(oid: String, params: Map[String, String]): Unit =
    hakukohteet += (oid -> params)

  def updateHakukohde(oid: String, jParams: java.util.Map[String, String]): Unit =
    hakukohteet += (oid -> (hakukohteet.getOrElse(oid, Map()) ++ jParams.asScala.toMap))

  def addValintaperuste(id: String, jParams: java.util.Map[String, String]): Unit =
    addValintaperuste(id, jParams.asScala.toMap)

  def addValintaperuste(id: String, params: Map[String, String]): Unit =
    valintaperusteet += (id -> params)

  def updateValintaperuste(id: String, jParams: java.util.Map[String, String]): Unit =
    valintaperusteet += (id -> (valintaperusteet.getOrElse(id, Map()) ++ jParams.asScala.toMap))

  def addSorakuvaus(id: String, jParams: java.util.Map[String, String]): Unit =
    addSorakuvaus(id, jParams.asScala.toMap)

  def addSorakuvaus(id: String, params: Map[String, String]): Unit =
    sorakuvaukset += (id -> params)

  def updateSorakuvaus(id: String, jParams: java.util.Map[String, String]): Unit =
    sorakuvaukset += (id -> (sorakuvaukset.getOrElse(id, Map()) ++ jParams.asScala.toMap))

  def addOppilaitos(oid: String, jParams: java.util.Map[String, String]): Unit =
    addOppilaitos(oid, jParams.asScala.toMap)

  def addOppilaitos(oid: String, params: Map[String, String]): Unit =
    oppilaitokset += (oid -> params)

  def updateOppilaitos(oid: String, jParams: java.util.Map[String, String]): Unit =
    oppilaitokset += (oid -> (oppilaitokset.getOrElse(oid, Map()) ++ jParams.asScala.toMap))

  def addOppilaitoksenOsa(oid: String, jParams: java.util.Map[String, String]): Unit =
    addOppilaitoksenOsa(oid, jParams.asScala.toMap)

  def addOppilaitoksenOsa(oid: String, params: Map[String, String]): Unit =
    oppilaitostenOsat += (oid -> params)

  def updateOppilaitoksenOsa(oid: String, jParams: java.util.Map[String, String]): Unit =
    oppilaitostenOsat += (oid -> (oppilaitostenOsat.getOrElse(oid, Map()) ++ jParams.asScala.toMap))

  def reset(): Unit = {
    koulutukset = Map()
    toteutukset = Map()
    haut = Map()
    hakukohteet = Map()
    valintaperusteet = Map()
    sorakuvaukset = Map()
    oppilaitokset = Map()
    oppilaitostenOsat = Map()
  }

  val KoulutusOidKey = "koulutusOid"
  val ToteutusOidKey = "toteutusOid"
  val HakuOidKey = "hakuOid"
  val HakukohdeOidKey = "hakukohdeOid"
  val ExternalIdKey = "externalId"
  val JohtaaTutkintoonKey = "johtaaTutkintoon"
  val KoulutustyyppiKey = "koulutustyyppi"
  val KoulutusKoodiUriKey = "koulutusKoodiUri"
  val KoulutuksetKoodiUriKey = "koulutuksetKoodiUri"
  val TilaKey = "tila"
  val TarjoajatKey = "tarjoajat"
  val NimiKey = "nimi"
  val EsitysNimiKey = "esitysnimi"
  val HakukohdeKoodiUriKey = "hakukohdeKoodiUri"
  val JulkinenKey = "julkinen"
  val EsikatseluKey = "esikatselu"
  val MuokkaajaKey = "muokkaaja"
  val OrganisaatioKey = "organisaatio"
  val KielivalintaKey = "kielivalinta"
  val ModifiedKey = "modified"
  val HakutapaKoodiUriKey = "hakutapaKoodiUri"
  val AlkamiskausiKoodiUriKey = "alkamiskausiKoodiUri"
  val AlkamisvuosiKey = "alkamisvuosi"
  val KaytetaanHaunAlkamiskauttaKey = "kaytetaanHaunAlkamiskautta"
  val KohdejoukkoKoodiUriKey = "kohdejoukkoKoodiUri"
  val KohdejoukonTarkenneKoodiUriKey = "kohdejoukonTarkenneKoodiUri"
  val HakulomaketyyppiKey = "hakulomaketyyppi"
  val HakulomakeIdKey = "hakulomakeAtaruId"
  val HakulomakeKuvausKey = "hakulomakeKuvaus"
  val HakulomakeLinkkiKey = "hakulomakeLinkki"
  val KaytetaanHaunHakulomakettaKey = "kaytetaanHaunHakulomaketta"
  val JarjestyspaikkaOidKey = "jarjestyspaikkaOid"
  val HakukohteenLiittamisenTakarajaKey = "hakukohteenLiittamisenTakaraja"
  val HakukohteenMuokkaamisenTakarajaKey = "hakukohteenMuokkaamisenTakaraja"
  val HakuaikaAlkaaKey = "hakuaikaAlkaa"
  val HakuaikaPaattyyKey = "hakuaikaPaattyy"
  val AloituspaikatKey = "aloituspaikat"
  val PohjakoulutusvaatimusKoodiUritKey = "pohjakoulutusvaatimusKoodiUrit"
  val ValintatapaKoodiUritKey = "valintatapaKoodiUritKey"
  val PohjakoulutusvaatimusTarkenneKey = "pohjakoulutusvaatimusTarkenne"
  val ToinenAsteOnkoKaksoistutkintoKey = "toinenAsteOnkoKaksoistutkinto"
  val KaytetaanHaunAikatauluaKey = "kaytetaanHaunAikataulua"
  val ValintaperusteIdKey = "valintaperuste"
  val LiitteetOnkoSamaToimitusaikaKey = "liitteetOnkoSamaToimitusaika"
  val LiitteetOnkoSamaToimitusosoiteKey = "liitteetOnkoSamaToimitusosoite"
  val LiitteidenToimitusaikaKey = "liitteidenToimitusaika"
  val LiitteidenToimitustapaKey = "liitteidenToimitustapa"
  val LiitteidenToimitusosoiteKey = "liitteidenToimitusosoite"
  val LiitteetKey = "liitteet"
  val ValintakokeetKey = "valintakokeet"
  val MuuPohjakoulutusvaatimusKey = "muuPohjakoulutusvaatimus"
  val AjastettuJulkaisuKey = "ajastettuJulkaisu"
  val MetadataKey = "metadata"
  val SorakuvausIdKey = "sorakuvausId"
  val OppilaitosOidKey = "oppilaitosOid"
  val TeemakuvaKey = "teemakuva"
  val LogoKey = "logo"
  val EPerusteIdKey = "ePerusteId"

  def formatModified(date: LocalDateTime): String = ISO_MODIFIED_FORMATTER.format(date)

  def parseModified(date: String): Modified = Modified(LocalDateTime.from(ISO_MODIFIED_FORMATTER.parse(date)))

  def formatLocalDateTime(date: LocalDateTime): String = ISO_LOCAL_DATE_TIME_FORMATTER.format(date)

  def parseLocalDateTime(date: String): LocalDateTime = LocalDateTime.from(ISO_LOCAL_DATE_TIME_FORMATTER.parse(date))

  def testDate(time: LocalTime, daysInFuture: Int): LocalDateTime =
    LocalDate.now().plusDays(daysInFuture).atTime(time).truncatedTo(ChronoUnit.MINUTES)

  def testDate(time: String, daysInFuture: Int): LocalDateTime =
    testDate(LocalTime.parse(time), daysInFuture)

  val startTime1: LocalDateTime = testDate("09:49", 1)
  val endTime1: LocalDateTime = testDate("09:58", 1)
  val time3: LocalDateTime = testDate("09:58", 3)
  val thisYear = LocalDate.now().getYear().toString

  val ammTutkinnonOsaKoulutusMetadata: String = write(TestData.AmmTutkinnonOsaKoulutus.metadata)
  val ammOsaamisalaKoulutusMetadata: String = write(TestData.AmmOsaamisalaKoulutus.metadata)
  val ammTutkinnonOsaToteutusMetadata: String = write(TestData.AmmTutkinnonOsaToteutus.metadata)
  val ammOsaamisalaToteutusMetadata: String = write(TestData.AmmOsaamisalaToteutus.metadata)
  val lukioToteutusMetadata: String = write(TestData.LukioToteutus.metadata)

  val DefaultKoulutusScala: Map[String, String] = Map[String, String](
    ExternalIdKey -> "987654321",
    JohtaaTutkintoonKey -> "true",
    KoulutustyyppiKey -> Amm.name,
    KoulutuksetKoodiUriKey -> "koulutus_371101#1",
    TilaKey -> Julkaistu.name,
    TarjoajatKey -> "1.2.246.562.10.67476956288, 1.2.246.562.10.594252633210",
    NimiKey -> "nimi",
    SorakuvausIdKey -> UUID.randomUUID().toString,
    JulkinenKey -> "false",
    EsikatseluKey -> "false",
    MuokkaajaKey -> TestUserOid.s,
    OrganisaatioKey -> OtherOid.s,
    KielivalintaKey -> "fi,sv",
    TeemakuvaKey -> "https://testi.fi/koulutus-teemakuva/oid/kuva.jpg",
    EPerusteIdKey -> "1234",
    ModifiedKey -> formatModified(LocalDateTime.now()),
    MetadataKey -> write(TestData.AmmKoulutus.metadata)
  )

  val DefaultKoulutus: java.util.Map[String, String] = mapAsJavaMap(DefaultKoulutusScala)

  val DefaultToteutusScala: Map[String, String] = Map[String, String](
    ExternalIdKey -> "1122334455",
    TilaKey -> Julkaistu.name,
    TarjoajatKey -> "1.2.246.562.10.67476956288, 1.2.246.562.10.594252633210",
    NimiKey -> "nimi",
    EsikatseluKey -> "false",
    MuokkaajaKey -> TestUserOid.s,
    OrganisaatioKey -> OtherOid.s,
    KielivalintaKey -> "fi,sv",
    TeemakuvaKey -> "https://testi.fi/toteutus-teemakuva/oid/kuva.jpg",
    ModifiedKey -> formatModified(LocalDateTime.now()),
    MetadataKey -> write(TestData.AmmToteutuksenMetatieto)
  )

  val DefaultToteutus: java.util.Map[String, String] = mapAsJavaMap(DefaultToteutusScala)

  val DefaultHakuScala: Map[String, String] = Map[String, String](
    ExternalIdKey -> "2233445566",
    TilaKey -> Julkaistu.name,
    NimiKey -> "nimi",
    MuokkaajaKey -> TestUserOid.s,
    OrganisaatioKey -> OtherOid.s,
    KielivalintaKey -> "fi,sv",
    ModifiedKey -> formatModified(LocalDateTime.now()),
    HakutapaKoodiUriKey -> "hakutapa_03#1",
    KohdejoukkoKoodiUriKey -> "haunkohdejoukko_02#2",
    KohdejoukonTarkenneKoodiUriKey -> "haunkohdejoukontarkenne_1#11",
    HakulomaketyyppiKey -> EiSähköistä.toString,
    HakulomakeIdKey -> "dcd38a87-912e-4e91-8840-99c7e242dd53",
    HakulomakeKuvausKey -> "Hakulomake tulostetaan ja toimitetaan postitse",
    HakulomakeLinkkiKey -> "https://koulu.test/hakemusinfo",
    HakukohteenLiittamisenTakarajaKey -> formatLocalDateTime(startTime1),
    HakukohteenMuokkaamisenTakarajaKey -> formatLocalDateTime(endTime1),
    AjastettuJulkaisuKey -> formatLocalDateTime(time3),
    HakuaikaAlkaaKey -> formatLocalDateTime(startTime1),
    HakuaikaPaattyyKey -> formatLocalDateTime(endTime1),
    MetadataKey -> write(TestData.JulkaistuHaku.metadata.get.copy(
      tulevaisuudenAikataulu = Seq(Ajanjakso(alkaa = startTime1, paattyy = Some(endTime1))),
      koulutuksenAlkamiskausi = Some(
        KoulutuksenAlkamiskausi(
          alkamiskausityyppi = Some(AlkamiskausiJaVuosi),
          koulutuksenAlkamiskausiKoodiUri = Some( "kausi_k#1"),
          koulutuksenAlkamisvuosi = Some(thisYear),
          koulutuksenAlkamispaivamaara = None,
          koulutuksenPaattymispaivamaara = None))))
  )

  val DefaultHaku: java.util.Map[String, String] = mapAsJavaMap(DefaultHakuScala)

  val DefaultHakukohdeScala: Map[String, String] = Map[String, String](
    ExternalIdKey -> "3344556677",
    TilaKey -> Julkaistu.name,
    NimiKey -> "nimi",
    EsitysNimiKey -> "esitysnimi",
    MuokkaajaKey -> TestUserOid.s,
    OrganisaatioKey -> OtherOid.s,
    KielivalintaKey -> "fi,sv",
    ModifiedKey -> formatModified(LocalDateTime.now()),
    HakulomaketyyppiKey -> EiSähköistä.toString,
    HakulomakeIdKey -> "369221b1-07d0-4b7a-89bc-04b670d8cff2",
    HakulomakeKuvausKey -> "Hakulomake tulostetaan ja toimitetaan postitse",
    HakulomakeLinkkiKey -> "https://koulu.test/hakemusinfo",
    KaytetaanHaunHakulomakettaKey -> "false",
    EsikatseluKey -> "false",
    JarjestyspaikkaOidKey -> OtherOid.s,
    HakuaikaAlkaaKey -> formatLocalDateTime(startTime1),
    HakuaikaPaattyyKey -> formatLocalDateTime(endTime1),
    PohjakoulutusvaatimusKoodiUritKey -> "pohjakoulutusvaatimuskouta_104#1, pohjakoulutusvaatimuskouta_109#1",
    PohjakoulutusvaatimusTarkenneKey -> "Pohjakoulutusvaatimuksen tarkenne",
    ToinenAsteOnkoKaksoistutkintoKey -> "false",
    KaytetaanHaunAikatauluaKey -> "false",
    HakuaikaAlkaaKey -> formatLocalDateTime(startTime1),
    HakuaikaPaattyyKey -> formatLocalDateTime(endTime1),
    ValintaperusteIdKey -> UUID.randomUUID().toString,
    MetadataKey -> write(TestData.JulkaistuHakukohde.metadata.get.copy(koulutuksenAlkamiskausi = Some(KoulutuksenAlkamiskausi(
      alkamiskausityyppi = Some(TarkkaAlkamisajankohta),
      koulutuksenAlkamisvuosi = None,
      koulutuksenAlkamiskausiKoodiUri = None,
      koulutuksenAlkamispaivamaara = Some(startTime1),
      koulutuksenPaattymispaivamaara = Some(endTime1))))),
    LiitteetOnkoSamaToimitusaikaKey -> "true",
    LiitteetOnkoSamaToimitusosoiteKey -> "false",
    LiitteidenToimitusaikaKey -> formatLocalDateTime(time3),
    LiitteetKey -> write(List(TestData.Liite1.copy(id = Some(UUID.fromString("de7e733b-36a2-4d3f-ac71-32ccae96dc32")),
      toimitusaika = Some(endTime1)),
      TestData.Liite2.copy(id = Some(UUID.fromString("59b5a1c9-316b-4007-a14e-cc2b617bab46"))))),
    ValintakokeetKey -> write(List(TestData.Valintakoe1.copy(
      id = Some(UUID.fromString("f50c7536-1c50-4fa8-b13c-514877be71a0")),
      tilaisuudet = List(TestData.Valintakoe1.tilaisuudet.head.copy(aika = Some(Ajanjakso(startTime1, Some(endTime1)))))
    )))
  )

  val DefaultHakukohde: java.util.Map[String, String] = mapAsJavaMap(DefaultHakukohdeScala)

  val DefaultValintaperusteScala: Map[String, String] = Map[String, String](
    ExternalIdKey -> "4455667788",
    KoulutustyyppiKey -> Amm.name,
    TilaKey -> Julkaistu.name,
    NimiKey -> "nimi",
    EsikatseluKey -> "false",
    MuokkaajaKey -> TestUserOid.s,
    OrganisaatioKey -> OtherOid.s,
    KielivalintaKey -> "fi,sv",
    ModifiedKey -> formatModified(LocalDateTime.now()),
    HakutapaKoodiUriKey -> "hakutapa_01#1",
    KohdejoukkoKoodiUriKey -> "haunkohdejoukko_11#1",
    JulkinenKey -> "false",
    MetadataKey -> write(TestData.AmmValintaperuste.metadata),
    ValintakokeetKey -> write(List(TestData.Valintakoe1.copy(
      id = Some(UUID.fromString("f50c7536-1c50-4fa8-b13c-514877be71a0")),
      tilaisuudet = List(TestData.Valintakoe1.tilaisuudet.head.copy(aika = Some(Ajanjakso(startTime1, Some(endTime1)))))
    )))
  )

  val DefaultValintaperuste: java.util.Map[String, String] = mapAsJavaMap(DefaultValintaperusteScala)

  val DefaultSorakuvausScala: Map[String, String] = Map[String, String](
    ExternalIdKey -> "5566778899",
    TilaKey -> Julkaistu.name,
    NimiKey -> "nimi",
    KoulutustyyppiKey -> Amm.name,
    MuokkaajaKey -> TestUserOid.s,
    OrganisaatioKey -> OphOid.s,
    KielivalintaKey -> "fi,sv",
    ModifiedKey -> formatModified(LocalDateTime.now()),
    JulkinenKey -> "false",
    MetadataKey -> write(TestData.AmmSorakuvaus.metadata.get),
    SorakuvausIdKey -> UUID.randomUUID().toString,
  )

  val DefaultSorakuvaus: java.util.Map[String, String] = mapAsJavaMap(DefaultSorakuvausScala)

  val DefaultOppilaitosScala: Map[String, String] = Map[String, String](
    TilaKey -> Julkaistu.name,
    MuokkaajaKey -> TestUserOid.s,
    OrganisaatioKey -> OtherOid.s,
    EsikatseluKey -> "false",
    KielivalintaKey -> "fi,sv",
    TeemakuvaKey -> "https://testi.fi/oppilaitos-teemakuva/oid/kuva.jpg",
    LogoKey -> "https://testi.fi/oppilaitos-logo/oid/logo.png",
    ModifiedKey -> formatModified(LocalDateTime.now()),
    MetadataKey -> write(TestData.JulkaistuOppilaitos.metadata.get)
  )

  val DefaultOppilaitos: java.util.Map[String, String] = mapAsJavaMap(DefaultOppilaitosScala)

  val DefaultOppilaitoksenOsaScala: Map[String, String] = Map[String, String](
    TilaKey -> Julkaistu.name,
    MuokkaajaKey -> TestUserOid.s,
    OrganisaatioKey -> OtherOid.s,
    EsikatseluKey -> "false",
    KielivalintaKey -> "fi,sv",
    TeemakuvaKey -> "https://testi.fi/oppilaitoksen-osa-teemakuva/oid/kuva.jpg",
    ModifiedKey -> formatModified(LocalDateTime.now()),
    MetadataKey -> write(TestData.JulkaistuOppilaitoksenOsa.metadata.get)
  )

  val DefaultOppilaitoksenOsa: java.util.Map[String, String] = mapAsJavaMap(DefaultOppilaitoksenOsaScala)

  private def toKielistetty(kielivalinta: Seq[Kieli], nimi: String): Kielistetty =
    if (nimi == null || nimi.isEmpty()) Map() else
      kielivalinta.map { k => (k, nimi + " " + k.toString) }.toMap
  private def toKielivalinta(params:Map[String, String]) = params(KielivalintaKey).split(",").map(_.trim).map(Kieli.withName)

  private def toJsonIfValid[T <: Validatable](v :T): String = v.validate() match {
    case NoErrors => toJson(v)
    case errors => throw new RuntimeException(s"""${errors.mkString(", ")}""")
  }

  def getKoulutus(oid:String): String = toJsonIfValid(koulutus(oid))

  def koulutus(oid:String): Koulutus = {
    val params = koulutukset(oid)
    val kielivalinta = toKielivalinta(params)
    Koulutus(
      Some(KoulutusOid(oid)),
      Some("987654321"),
      params(JohtaaTutkintoonKey).toBoolean,
      Koulutustyyppi.withName(params(KoulutustyyppiKey)),
      params.get(KoulutuksetKoodiUriKey) match {
        case None | Some(null) => Seq()
        case Some(x) => x.split(",").map(_.trim).toSeq
      },
      Julkaisutila.withName(params(TilaKey)),
      params.get(EsikatseluKey) match {
        case None => false
        case Some(x) => x.toBoolean
      },
      params.get(TarjoajatKey) match {
        case None => List[OrganisaatioOid]()
        case Some(x) if x.trim == "" => List[OrganisaatioOid]()
        case Some(x) => x.split(",").map(_.trim).map(OrganisaatioOid).toList
      },
      toKielistetty(kielivalinta, params(NimiKey)),
      Some(UUID.fromString(params(SorakuvausIdKey))),
      params.get(MetadataKey).map(read[KoulutusMetadata]),
      params(JulkinenKey).toBoolean,
      UserOid(params(MuokkaajaKey)),
      OrganisaatioOid(params(OrganisaatioKey)),
      kielivalinta,
      params.get(TeemakuvaKey),
      params.get(EPerusteIdKey) match {
        case None | Some(null) => None
        case Some(x) => Some(x.toLong)
      },
      Some(parseModified(params(ModifiedKey))))
  }

  def getToteutus(oid:String): String = toJsonIfValid(toteutus(oid))

  private def toteutus(oid:String) = {
    val params = toteutukset(oid)
    val kielivalinta = toKielivalinta(params)
    Toteutus(
      Some(ToteutusOid(oid)),
      Some("1122334455"),
      KoulutusOid(params(KoulutusOidKey)),
      Julkaisutila.withName(params(TilaKey)),
      params.get(EsikatseluKey) match {
        case None => false
        case Some(x) => x.toBoolean
      },
      params(TarjoajatKey).split(",").map(_.trim).map(OrganisaatioOid).toList,
      toKielistetty(kielivalinta, params(NimiKey)),
      params.get(MetadataKey).map(read[ToteutusMetadata]),
      None,
      UserOid(params(MuokkaajaKey)),
      OrganisaatioOid(params(OrganisaatioKey)),
      kielivalinta,
      params.get(TeemakuvaKey),
      Some(parseModified(params(ModifiedKey))))
  }

  def getHaku(oid:String): String = {
    val params = haut(oid)
    val kielivalinta = toKielivalinta(params)
    toJsonIfValid( Haku(
      Some(HakuOid(oid)),
      Some("2233445566"),
      Julkaisutila.withName(params(TilaKey)),
      toKielistetty(kielivalinta, params(NimiKey)),
      Some(params(HakutapaKoodiUriKey)),
      Some(parseLocalDateTime(params(HakukohteenLiittamisenTakarajaKey))),
      Some(parseLocalDateTime(params(HakukohteenMuokkaamisenTakarajaKey))),
      params.get(AjastettuJulkaisuKey).map(parseLocalDateTime),
      Some(params(KohdejoukkoKoodiUriKey)),
      params.get(KohdejoukonTarkenneKoodiUriKey).flatMap(Option(_)),
      Some(Hakulomaketyyppi.withName(params(HakulomaketyyppiKey))),
      params.get(HakulomakeIdKey).map(UUID.fromString),
      toKielistetty(kielivalinta, params(HakulomakeKuvausKey)),
      toKielistetty(kielivalinta, params(HakulomakeLinkkiKey)),
      params.get(MetadataKey).map(read[HakuMetadata]),
      OrganisaatioOid(params(OrganisaatioKey)),
      List(Ajanjakso(parseLocalDateTime(params(HakuaikaAlkaaKey)), Some(parseLocalDateTime(params(HakuaikaPaattyyKey))))),
      UserOid(params(MuokkaajaKey)),
      params(KielivalintaKey).split(",").map(_.trim).map(Kieli.withName),
      Some(parseModified(params(ModifiedKey)))))
  }

  def getHakukohde(oid:String): String = {
    val params = hakukohteet(oid)
    val kielivalinta = toKielivalinta(params)
    val nimi = if ("{}".equals(params(NimiKey))) "" else params(NimiKey)
    val esitysnimi = if ("{}".equals(params(EsitysNimiKey))) "" else params(EsitysNimiKey)
    toJsonIfValid( Hakukohde(
      Some(HakukohdeOid(oid)),
      Some("3344556677"),
      ToteutusOid(params(ToteutusOidKey)),
      HakuOid(params(HakuOidKey)),
      Julkaisutila.withName(params(TilaKey)),
      params.get(EsikatseluKey) match {
        case None => false
        case Some(x) => x.toBoolean
      },
      toKielistetty(kielivalinta, nimi),
      params.get(HakukohdeKoodiUriKey),
      Some(OrganisaatioOid(params(JarjestyspaikkaOidKey))),
      Some(Hakulomaketyyppi.withName(params(HakulomaketyyppiKey))),
      params.get(HakulomakeIdKey).map(UUID.fromString),
      toKielistetty(kielivalinta, params(HakulomakeKuvausKey)),
      toKielistetty(kielivalinta, params(HakulomakeLinkkiKey)),
      Some(params(KaytetaanHaunHakulomakettaKey).toBoolean),
      params(PohjakoulutusvaatimusKoodiUritKey).split(",").map(_.trim).toSeq,
      toKielistetty(kielivalinta, params(PohjakoulutusvaatimusTarkenneKey)),
      params.get(MuuPohjakoulutusvaatimusKey).map(toKielistetty(kielivalinta, _)).getOrElse(Map()),
      Some(params(ToinenAsteOnkoKaksoistutkintoKey).toBoolean),
      Some(params(KaytetaanHaunAikatauluaKey).toBoolean),
      Some(UUID.fromString(params(ValintaperusteIdKey))),
      Some(params(LiitteetOnkoSamaToimitusaikaKey).toBoolean),
      Some(params(LiitteetOnkoSamaToimitusosoiteKey).toBoolean),
      params.get(LiitteidenToimitusaikaKey).map(parseLocalDateTime),
      params.get(LiitteidenToimitustapaKey).map(LiitteenToimitustapa.withName),
      params.get(LiitteidenToimitusosoiteKey).map(read[LiitteenToimitusosoite]),
      params.get(LiitteetKey).map(read[List[Liite]]).getOrElse(List()),
      params.get(ValintakokeetKey).map(read[List[Valintakoe]]).getOrElse(List()),
      List(Ajanjakso(parseLocalDateTime(params(HakuaikaAlkaaKey)), Some(parseLocalDateTime(params(HakuaikaPaattyyKey))))),
      params.get(MetadataKey).map(read[HakukohdeMetadata]),
      UserOid(params(MuokkaajaKey)),
      OrganisaatioOid(params(OrganisaatioKey)),
      params(KielivalintaKey).split(",").map(_.trim).map(Kieli.withName),
      Some(parseModified(params(ModifiedKey))),
      Some(EnrichedData(toKielistetty(kielivalinta, esitysnimi), muokkaajanNimi = Some("Testi Muokkaaja")))))
  }

  def getValintaperuste(id:String): String = {
    val params = valintaperusteet(id)
    val kielivalinta = toKielivalinta(params)
    toJsonIfValid( Valintaperuste(
      Some(UUID.fromString(id)),
      Some("4455667788"),
      Julkaisutila.withName(params(TilaKey)),
      params.get(EsikatseluKey) match {
        case None => false
        case Some(x) => x.toBoolean
      },
      Koulutustyyppi.withName(params(KoulutustyyppiKey)),
      Some(params(HakutapaKoodiUriKey)),
      Some(params(KohdejoukkoKoodiUriKey)),
      toKielistetty(kielivalinta, params(NimiKey)),
      params(JulkinenKey).toBoolean,
      params.get(ValintakokeetKey).map(read[List[Valintakoe]]).getOrElse(List()),
      params.get(MetadataKey).map(read[ValintaperusteMetadata]),
      OrganisaatioOid(params(OrganisaatioKey)),
      UserOid(params(MuokkaajaKey)),
      params(KielivalintaKey).split(",").map(_.trim).map(Kieli.withName),
      Some(parseModified(params(ModifiedKey)))
    ))
  }

  def getSorakuvaus(id:String): String = {
    val params = sorakuvaukset(id)
    val kielivalinta = toKielivalinta(params)
    toJsonIfValid( Sorakuvaus(
      Some(UUID.fromString(id)),
      Some("5566778899"),
      Julkaisutila.withName(params(TilaKey)),
      toKielistetty(kielivalinta, params(NimiKey)),
      Koulutustyyppi.withName(params(KoulutustyyppiKey)),
      params(KielivalintaKey).split(",").map(_.trim).map(Kieli.withName),
      params.get(MetadataKey).map(read[SorakuvausMetadata]),
      OrganisaatioOid(params(OrganisaatioKey)),
      UserOid(params(MuokkaajaKey)),
      Some(parseModified(params(ModifiedKey)))
    ))
  }

  def getOppilaitos(oid:String): String = {
    val params = oppilaitokset(oid)
    toJsonIfValid( Oppilaitos(
      OrganisaatioOid(oid),
      Julkaisutila.withName(params(TilaKey)),
      params.get(EsikatseluKey) match {
        case None => false
        case Some(x) => x.toBoolean
      },
      params.get(MetadataKey).map(read[OppilaitosMetadata]),
      params(KielivalintaKey).split(",").map(_.trim).map(Kieli.withName),
      OrganisaatioOid(params(OrganisaatioKey)),
      UserOid(params(MuokkaajaKey)),
      params.get(TeemakuvaKey),
      params.get(LogoKey),
      Some(parseModified(params(ModifiedKey)))
    ))
  }

  def getOppilaitoksenOsa(oid:String): String = toJsonIfValid(oppilaitoksenOsa(oid))

  private def oppilaitoksenOsa(oid:String) = {
    val params = oppilaitostenOsat(oid)
    OppilaitoksenOsa(
      OrganisaatioOid(oid),
      OrganisaatioOid(params(OppilaitosOidKey)),
      Julkaisutila.withName(params(TilaKey)),
      params.get(EsikatseluKey) match {
        case None => false
        case Some(x) => x.toBoolean
      },
      params.get(MetadataKey).map(read[OppilaitoksenOsaMetadata]),
      params(KielivalintaKey).split(",").map(_.trim).map(Kieli.withName),
      OrganisaatioOid(params(OrganisaatioKey)),
      UserOid(params(MuokkaajaKey)),
      params.get(TeemakuvaKey),
      Some(parseModified(params(ModifiedKey)))
    )
  }

  def getToteutuksetByKoulutus(koulutusOid: String, vainJulkaistut: Boolean): String = {
    toJson( toteutukset.filter {
        case (_, params) => params(KoulutusOidKey) == koulutusOid && (!vainJulkaistut || params(TilaKey) == Julkaistu.name)
      }.map {
        case (oid, _) => toteutus(oid)
      }
    )
  }

  def listHakukohteetByHaku(hakuOid: String): String = {
    toJson(
      hakukohteet.filter {
        case (_, params) => params(HakuOidKey) == hakuOid
      }.map {
        case (oid, _) => hakukohdeListItem(oid)
      }
    )
  }

  def listHautByToteutus(toteutusOid: String): String = {
    toJson(
      hakukohteet.filter {
        case (_, params) => params(ToteutusOidKey) == toteutusOid
      }.map {
        case (_, params) => params(HakuOidKey)
      }.toSeq.map(hakuListItem)
    )
  }

  def listHakukohteetByToteutus(toteutusOid: String): String = {
    toJson(
      hakukohteet.filter {
        case (_, params) => params(ToteutusOidKey) == toteutusOid
      }.map {
        case (oid, _) => oid
      }.toSeq.map(hakukohdeListItem)
    )
  }

  def listKoulutuksetByHaku(hakuOid: String): String = {
    toJson(
      hakukohteet.filter {
        case (_, params) => params(HakuOidKey) == hakuOid
      }.map {
        case (_, params) => params(ToteutusOidKey)
      }.map {
        oid => koulutusListItem(toteutukset(oid)(KoulutusOidKey))
      }
    )
  }

  def getKoulutuksetByTarjoajat(tarjoajaOids: String): String = {
    val oids = tarjoajaOids.split(',')
    toJson(
      koulutukset.filter {
        case (_, params) => !params(TarjoajatKey).split(',').intersect(oids).isEmpty && params(TilaKey) == Julkaistu.name
      }.map {
        case (oid, _) => koulutus(oid)
      }
    )
  }

  def getHakukohdeOidsByJarjestyspaikat(jarjestyspaikkaOids: String): String = {
    val oids = jarjestyspaikkaOids.split(',')
    toJson(
      hakukohteet.filter {
        case (_, params) => !params(JarjestyspaikkaOidKey).split(',').intersect(oids).isEmpty && params(TilaKey) == Julkaistu.name
      }.map {
        case (oid, _) => oid
      }
    )
  }

  def listToteutuksetByHaku(hakuOid: String): String = {
    toJson(
      hakukohteet.filter {
        case (_, params) => params(HakuOidKey) == hakuOid
      }.map {
        case (_, params) => params(ToteutusOidKey)
      }.toSeq.map(toteutusListItem)
    )
  }

  private def isHakuExistingAndNonArkistoitu(oid: String): Boolean = {
    val tila = haut(oid).getOrElse(TilaKey, "")
    tila != Poistettu.name && tila != Arkistoitu.name
  }

  def getHakutiedotByKoulutus(koulutusOid: String): String = {
    toJson(
      toteutukset
        .filter { case (_, params) => params(KoulutusOidKey) == koulutusOid &&
          params(TilaKey) != Arkistoitu.name && params(TilaKey) != Poistettu.name }
        .map { case (oid, _) =>
          Hakutieto(
            ToteutusOid(oid),
            hakukohteet
              .filter  { case (_, params) => params(ToteutusOidKey) == oid &&
                params(TilaKey) != Arkistoitu.name && params(TilaKey) != Poistettu.name &&
                isHakuExistingAndNonArkistoitu(params(HakuOidKey))
              }
              .groupBy { case (_, params) => params(HakuOidKey) }
              .map     { case (hakuOid, hakukohteet) =>
                hakutietoHaku(hakuOid).copy(
                  hakukohteet = hakukohteet.map { case (oid, _) => hakutietoHakukohde(oid) }.toSeq
                )
              }.toSeq
          )
        }
    )
  }

  def listKoulutusOidsBySorakuvaus(sorakuvausId: String): String = {
    toJson(
      koulutukset.filter {
        case (_, params) => params(SorakuvausIdKey) == sorakuvausId
      }.keys.toSeq
    )
  }

  def listHakukohteetByValintaperuste(valintaperusteId: String): String = {
    toJson(
      hakukohteet.filter {
        case (_, params) => params(ValintaperusteIdKey) == valintaperusteId
      }.keys.toSeq.map(hakukohdeListItem)
    )
  }

  def getOppilaitostenOsatByOppilaitos(oppilaitosOid: String): String = {
    toJson( oppilaitostenOsat.filter {
      case (_, params) => params(OppilaitosOidKey) == oppilaitosOid
    }.map {
      case (oid, _) => oppilaitoksenOsa(oid)
    })
  }

  def listOppilaitostenOsatByOppilaitos(oppilaitosOid: String): String = {
    toJson( oppilaitostenOsat.filter {
      case (_, params) => params(OppilaitosOidKey) == oppilaitosOid
    }.keys.toSeq.map(oppilaitoksenOsaListItem))
  }

  def getLastModified(since:String): String = {
    toJson(
      ListEverything(
        koulutukset.keySet.map(KoulutusOid).toSeq,
        toteutukset.keySet.map(ToteutusOid).toSeq,
        haut.keySet.map(HakuOid).toSeq,
        hakukohteet.keySet.map(HakukohdeOid).toSeq,
        valintaperusteet.keySet.map(UUID.fromString).toSeq,
        oppilaitostenOsat.keySet.map(OrganisaatioOid).toSeq.union(
          oppilaitokset.keySet.map(OrganisaatioOid).toSeq
        )
      )
    )
  }

  private def hakukohdeListItem(oid:String) = {
    val params = hakukohteet(oid)
    val kielivalinta = toKielivalinta(params)
    HakukohdeListItem(
      HakukohdeOid(oid),
      ToteutusOid(params(ToteutusOidKey)),
      HakuOid(params(HakuOidKey)),
      Some(UUID.fromString(params(ValintaperusteIdKey))),
      toKielistetty(kielivalinta, params(NimiKey)),
      params.get(HakukohdeKoodiUriKey),
      Julkaisutila.withName(params(TilaKey)),
      Some(OrganisaatioOid(params(JarjestyspaikkaOidKey))),
      OrganisaatioOid(params(OrganisaatioKey)),
      UserOid(params(MuokkaajaKey)),
      parseModified(params(ModifiedKey))
    )
  }

  private def hakuListItem(oid: String) = {
    val params = haut(oid)
    val kielivalinta = toKielivalinta(params)
    HakuListItem(
      HakuOid(oid),
      toKielistetty(kielivalinta, params(NimiKey)),
      Julkaisutila.withName(params(TilaKey)),
      OrganisaatioOid(params(OrganisaatioKey)),
      UserOid(params(MuokkaajaKey)),
      parseModified(params(ModifiedKey))
    )
  }

  private def koulutusListItem(oid:String) = {
    val params = koulutukset(oid)
    val kielivalinta = toKielivalinta(params)
    KoulutusListItem(
      KoulutusOid(oid),
      toKielistetty(kielivalinta, params(NimiKey)),
      Julkaisutila.withName(params(TilaKey)),
      params(TarjoajatKey).split(",").map(_.trim).map(OrganisaatioOid).toList,
      OrganisaatioOid(params(OrganisaatioKey)),
      UserOid(params(MuokkaajaKey)),
      parseModified(params(ModifiedKey))
    )
  }

  private def toteutusListItem(oid:String) = {
    val params = toteutukset(oid)
    val kielivalinta = toKielivalinta(params)
    ToteutusListItem(
      ToteutusOid(oid),
      KoulutusOid(params(KoulutusOidKey)),
      toKielistetty(kielivalinta, params(NimiKey)),
      Julkaisutila.withName(params(TilaKey)),
      params(TarjoajatKey).split(",").map(_.trim).map(OrganisaatioOid).toList,
      OrganisaatioOid(params(OrganisaatioKey)),
      UserOid(params(MuokkaajaKey)),
      parseModified(params(ModifiedKey)),
    )
  }

  private def oppilaitoksenOsaListItem(oid: String) = {
    val params = oppilaitostenOsat(oid)
    OppilaitoksenOsaListItem(
      OrganisaatioOid(oid),
      OrganisaatioOid(params(OppilaitosOidKey)),
      Julkaisutila.withName(params(TilaKey)),
      OrganisaatioOid(params(OrganisaatioKey)),
      UserOid(params(MuokkaajaKey)),
      parseModified(params(ModifiedKey))
    )
  }

  private def hakutietoHaku(oid:String) = {
    val params = haut(oid)
    val kielivalinta = toKielivalinta(params)
    val koulutuksenAlkamiskausi = getAlkamiskausiFromHakuMetadata(params)

    HakutietoHaku(
      HakuOid(oid),
      toKielistetty(kielivalinta, params(NimiKey)),
      Some(params(HakutapaKoodiUriKey)),
      Julkaisutila.withName(params(TilaKey)),
      koulutuksenAlkamiskausi,
      Some(Hakulomaketyyppi.withName(params(HakulomaketyyppiKey))),
      params.get(HakulomakeIdKey).map(UUID.fromString),
      toKielistetty(kielivalinta, params(HakulomakeKuvausKey)),
      toKielistetty(kielivalinta, params(HakulomakeLinkkiKey)),
      OrganisaatioOid(params(OrganisaatioKey)),
      List(Ajanjakso(parseLocalDateTime(params(HakuaikaAlkaaKey)), Some(parseLocalDateTime(params(HakuaikaPaattyyKey))))),
      UserOid(params(MuokkaajaKey)),
      Some(parseModified(params(ModifiedKey))),
      List()
    )
  }

  private def getAlkamiskausiFromHakuMetadata(params: Map[String, String]) = {
    params
      .get(MetadataKey)
      .map(read[HakuMetadata])
      .flatMap(_.koulutuksenAlkamiskausi)
  }

  def getAlkamiskausiFromHakukohdeMetadata(params: Map[String, String]): (Option[KoulutuksenAlkamiskausi], Option[Boolean]) = {
    val metadata = params
      .get(MetadataKey)
      .map(read[HakukohdeMetadata])
      .get

    (metadata.koulutuksenAlkamiskausi, metadata.kaytetaanHaunAlkamiskautta)
  }

  private def hakutietoHakukohde(oid:String) = {
    val params = hakukohteet(oid)
    val valintaperusteMetadata = params
      .get(ValintaperusteIdKey)
      .flatMap(valintaperusteId => valintaperusteet.get(valintaperusteId))
      .flatMap(valintaperuste => valintaperuste.get(MetadataKey))
      .map(read[ValintaperusteMetadata])
    val kielivalinta = toKielivalinta(params)
    val (koulutuksenAlkamiskausi, kaytetaanHaunAlkamiskautta) = getAlkamiskausiFromHakukohdeMetadata(params)

    HakutietoHakukohde(
      HakukohdeOid(oid),
      ToteutusOid(params(ToteutusOidKey)),
      HakuOid(params(HakuOidKey)),
      toKielistetty(kielivalinta, params(NimiKey)),
      params.get(HakukohdeKoodiUriKey),
      Julkaisutila.withName(params(TilaKey)),
      params.get(EsikatseluKey) match {
        case None => false
        case Some(x) => x.toBoolean
      },
      params.get(ValintaperusteIdKey).map(UUID.fromString),
      koulutuksenAlkamiskausi,
      kaytetaanHaunAlkamiskautta,
      Some(OrganisaatioOid(params(JarjestyspaikkaOidKey))),
      false,
      Some(Hakulomaketyyppi.withName(params(HakulomaketyyppiKey))),
      params.get(HakulomakeIdKey).map(UUID.fromString),
      toKielistetty(kielivalinta, params(HakulomakeKuvausKey)),
      toKielistetty(kielivalinta, params(HakulomakeLinkkiKey)),
      params.get(KaytetaanHaunHakulomakettaKey).map(_.toBoolean),
      params.get(MetadataKey).map(read[HakukohdeMetadata]).get.aloituspaikat,
      params.get(MetadataKey).map(read[HakukohdeMetadata]).get.hakukohteenLinja,
      Some(params(KaytetaanHaunAikatauluaKey).toBoolean),
      List(Ajanjakso(parseLocalDateTime(params(HakuaikaAlkaaKey)), Some(parseLocalDateTime(params(HakuaikaPaattyyKey))))),
      params.get(PohjakoulutusvaatimusKoodiUritKey) match {
        case None => List[String]()
        case Some(x) if x.trim == "" => List[String]()
        case Some(x)  => x.split(",").map(_.trim).toList
      },
      toKielistetty(kielivalinta, params(PohjakoulutusvaatimusTarkenneKey)),
      UserOid(params(MuokkaajaKey)),
      OrganisaatioOid(params(OrganisaatioKey)),
      valintaperusteMetadata match {
        case None => List[String]()
        case Some(x) => x.valintatavat.flatMap(_.valintatapaKoodiUri).toList
      },
      Some(parseModified(params(ModifiedKey))),
      params.get(MetadataKey).map(read[ToteutusMetadata]),
    )
  }
}
