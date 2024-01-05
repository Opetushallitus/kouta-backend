package fi.oph.kouta.validation

import fi.oph.kouta.client.HakemusPalveluClient
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.filterTypes.koulutusTyyppi
import fi.oph.kouta.domain.oid.{Oid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.service.KoodistoService
import fi.oph.kouta.validation.CrudOperations.{CrudOperation, update}
import fi.oph.kouta.validation.ExternalQueryResults.{ExternalQueryResult, itemFound, queryFailed}
import org.apache.commons.validator.routines.{EmailValidator, UrlValidator}

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID
import java.util.regex.Pattern

object Validations {
  private val urlValidator   = new UrlValidator(Array("http", "https"))
  private val emailValidator = EmailValidator.getInstance(false, false)

  def error(path: String, msg: ErrorMessage): IsValid = List(ValidationError(path, msg))

  def and(validations: IsValid*): IsValid          = validations.flatten.distinct
  def or(first: IsValid, second: IsValid): IsValid = if (first.isEmpty) second else first

  def validationMsg(value: String): ErrorMessage = ErrorMessage(msg = s"'$value' ei ole validi", id = "validationMsg")
  val notEmptyMsg: ErrorMessage                  = ErrorMessage(msg = s"Ei saa sisältää arvoa", id = "notEmptyMsg")
  val missingMsg: ErrorMessage                   = ErrorMessage(msg = s"Pakollinen tieto puuttuu", id = "missingMsg")
  val notNegativeMsg: ErrorMessage               = ErrorMessage(msg = s"ei voi olla negatiivinen", id = "notNegativeMsg")
  val tooManyKoodiUris: ErrorMessage =
    ErrorMessage(msg = s"Ainoastaan korkeakoulutuksella voi olla useampi kuin yksi koulutus", id = "tooManyKoodiUris")
  val withoutLukiolinja: ErrorMessage =
    ErrorMessage(msg = "Lukio-toteutuksella täytyy olla vähintään yleislinja", id = "withoutLukiolinja")
  val illegalHaunLomaketyyppiForHakukohdeSpecificTyyppi = ErrorMessage(
    msg =
      "Hakukohteelle ei voi valita erillistä hakulomaketta. Erillisen lomakkeen voi valita vain jos haussa on käytössä 'muu'-tyyppinen hakulomake.",
    id = "illegalHaunLomaketyyppiForHakukohdeSpecificTyyppi"
  )
  val toinenAsteOnkoKaksoistutkintoNotAllowed = ErrorMessage(
    msg =
      "Hakukohteelle ei voi valita kaksoistutkinnon suorittamista, kaksoistutkinto on mahdollinen vain jos kyseessä on ammatillinen perustutkinto tai lukiokoulutus (joko lukion oppimäärä tai ylioppilastutkinto, koulutuskoodi 309902 tai 301101).",
    id = "toinenAsteOnkoKaksoistutkintoNotAllowed"
  )
  def invalidKoulutuskoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Koulutuskoodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidKoulutuskoodiuri"
  )

  def valintakoeIsNotFoundFromAllowedRelations(valintakoetyypinKoodiUri: String) = ErrorMessage(
    msg =
      s"Hakukohteella on valintakokeen tyyppi $valintakoetyypinKoodiUri, mitä ei saa hakukohteen haun (kohdejoukko tai hakutapa) tai koulutuksen (koulutuskoodi) tietojen mukaan valita",
    id = "valintakoeIsNotFoundFromAllowedRelations"
  )

  def invalidKoulutustyyppiKoodiForAmmatillinenPerustutkintoErityisopetuksena(
      koulutustyyppiKoodi: String
  ): ErrorMessage = ErrorMessage(
    msg =
      s"Koulutuksen koulutustyyppi $koulutustyyppiKoodi on virheellinen, koulutustyyppillä täytyy olla koodistorelaatio tyyppiin ${AmmatillisetPerustutkintoKoodit.koulutusTyypit.head} että se voidaan järjestää erityisopetuksena",
    id = "invalidKoulutustyyppiKoodiForAmmatillinenPerustutkintoErityisopetuksena"
  )

  def invalidLisatietoOtsikkoKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Lisätieto-otsikkokoodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidLisatietoOtsikkoKoodiuri"
  )
  def invalidKoulutusAlaKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Koulutusalakoodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidKoulutusAlaKoodiuri"
  )

  def invalidOpintojenLaajuusyksikkoKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Valittua opintojenlaajuusyksikko-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidOpintojenLaajuusyksikkoKoodiuri"
  )
  def invalidErikoistumiskoulutusKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Koulutukselle valittua erikoistumiskoulutus-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidErikoistumiskoulutusKoodiuri"
  )

  def invalidKoulutusOpintojenLaajuusyksikkoIntegrity(koodiUri: String, toteutukset: Seq[ToteutusOid]): ErrorMessage =
    ErrorMessage(
      msg =
        s"Ainakin yhdellä Koulutukseen liitetyllä toteutuksella on eri opintojenlaajuusyksikko-koodiUri kuin koulutuksella ($koodiUri).",
      id = "invalidKoulutusOpintojenLaajuusyksikkoIntegrity",
      meta = Some(Map("toteutukset" -> toteutukset))
    )

  def invalidOpinnonTyyppiKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Koulutukselle valittua opinnontyyppi-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidOpinnonTyyppiKoodiuri"
  )

  def invalidKoulutusOpintojenLaajuusNumeroIntegrity(
      laajuusMin: Option[Double],
      laajuusMax: Option[Double],
      toteutukset: Seq[ToteutusOid]
  ): ErrorMessage = ErrorMessage(
    msg =
      s"Ainakin yhdellä koulutukseen liitetyllä julkaistulla toteutuksella opintojen laajuus ei ole koulutuksella määritellyllä välillä (${laajuusMin
        .getOrElse("")} - ${laajuusMax.getOrElse("")})",
    id = "invalidKoulutusOpintojenLaajuusNumeroIntegrity",
    meta = Some(Map("toteutukset" -> toteutukset))
  )

  def invalidToteutusOpintojenLaajuusMin(koulutusLaajuusMin: Option[Double], toteutusLaajuusMin: Option[Double]) = {
    ErrorMessage(
      msg = s"Toteutuksen laajuuden minimi ${toteutusLaajuusMin
        .getOrElse("")} on pienempi kuin koulutuksen laajuuden minimi ${koulutusLaajuusMin.getOrElse("")}",
      id = "invalidToteutusOpintojenLaajuusMin"
    )
  }

  def invalidToteutusOpintojenLaajuusMax(koulutusLaajuusMax: Option[Double], toteutusLaajuusMax: Option[Double]) = {
    ErrorMessage(
      msg = s"Toteutuksen laajuuden maksimi ${toteutusLaajuusMax
        .getOrElse("")} on suurempi kuin koulutuksen laajuuden maksimi ${koulutusLaajuusMax.getOrElse("")}",
      id = "invalidToteutusOpintojenLaajuusMax"
    )
  }

  def invalidToteutusOpintojenLaajuusyksikkoIntegrity(
      koulutusLaajuusyksikkoKoodiUri: Option[String],
      toteutusLaajuusyksikkoKoodiUri: Option[String]
  ) = {
    ErrorMessage(
      msg = s"Toteutuksella on eri opintojen laajuusyksikkö (${toteutusLaajuusyksikkoKoodiUri
        .getOrElse("-")}) kuin koulutuksella (${koulutusLaajuusyksikkoKoodiUri.getOrElse("-")})",
      id = "invalidToteutusOpintojenLaajuusyksikkoIntegrity"
    )
  }

  def invalidKoulutustyyppiForLiitettyOpintojakso(toteutukset: Seq[ToteutusOid]) = {
    ErrorMessage(
      msg =
        s"Ainakin yhdellä opintokokonaisuuteen liitetyllä toteutuksella on väärä koulutustyyppi. Kaikkien toteutusten tulee olla opintojaksoja.",
      id = "invalidKoulutustyyppiForLiitettyOpintojakso",
      meta = Some(Map("toteutukset" -> toteutukset))
    )
  }

  def invalidTilaForLiitettyOpintojaksoOnJulkaisu(toteutukset: Seq[ToteutusOid]) = {
    ErrorMessage(
      msg =
        s"Ainakin yhdellä opintokokonaisuuteen liitetyllä toteutuksella on väärä julkaisutila. Kaikkien julkaistuun opintokokonaisuuteen liitettyjen opintojaksojen tulee olla julkaistuja.",
      id = "invalidTilaForLiitettyOpintojaksoOnJulkaisu",
      meta = Some(Map("toteutukset" -> toteutukset))
    )
  }

  def invalidTilaForLiitettyOpintojakso(toteutukset: Seq[ToteutusOid]) = {
    ErrorMessage(
      msg =
        s"Ainakin yhdellä opintokokonaisuuteen liitetyllä toteutuksella on väärä julkaisutila. Opintokokonaisuuteen liitettyjen opintojaksojen tulee olla luonnostilaisia tai julkaistuja.",
      id = "invalidTilaForLiitettyOpintojakso",
      meta = Some(Map("toteutukset" -> toteutukset))
    )
  }

  def unknownOpintojakso(toteutukset: Seq[ToteutusOid]) = {
    ErrorMessage(
      msg = s"Opintokokonaisuuteen liitettyä opintojaksoa ei löydy.",
      id = "unknownOpintojakso",
      meta = Some(Map("toteutukset" -> toteutukset))
    )
  }

  val cannotChangeIsAvoinKorkeakoulutus = ErrorMessage(
    id = "cannotChangeIsAvoinKorkeakoulutus",
    msg = "Avoimen korkeakoulutuksen valintaa ei voi enää muuttaa, koska koulutukseen on liitetty toteutuksia."
  )

  def cannotRemoveTarjoajaFromAvoinKorkeakoulutus(tarjoajat: List[OrganisaatioOid]) = ErrorMessage(
    id = "cannotRemoveTarjoajaFromAvoinKorkeakoulutus",
    msg =
      s"Avoimen korkeakoulutuksen tarjoajaa ei voi enää poistaa, koska koulutukseen on liitetty toteutuksia, joissa tarjoaja on lisätty järjestäjäksi: ${tarjoajat
        .mkString(", ")}."
  )

  val invalidIsAvoinKorkeakoulutusIntegrity =
    ErrorMessage(
      id = "invalidIsAvoinKorkeakoulutusIntegrity",
      msg =
        "Toteutuksen voi tallentaa avoimena korkeakoulutuksena vain jos sen koulutus on myös avointa korkeakoulutusta."
    )

  def invalidJarjestajaForAvoinKorkeakoulutus(invalidJarjestajat: List[OrganisaatioOid]) =
    ErrorMessage(
      id = "invalidJarjestajaForAvoinKorkeakoulutus",
      msg =
        s"Toteutukselle ei voi lisätä järjestäjiksi seuraavia organisaatioita, joita ei ole lisätty koulutuksen järjestäjiksi: ${invalidJarjestajat
          .mkString(", ")}."
    )

  def invalidKieliKoodiUri(kieliField: String, koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Lukiototeutukselle valittua $kieliField-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    "invalidKieliKoodiUri"
  )
  def invalidLukioLinjaKoodiUri(linjaField: String, koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Lukiototeutukselle valittua $linjaField-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    "invalidLukioLinjaKoodiUri"
  )
  def tarjoajaOidWoRequiredKoulutustyyppi(oid: OrganisaatioOid, koulutustyyppi: Koulutustyyppi): ErrorMessage =
    ErrorMessage(
      msg = s"Tarjoaja-organisaatiolla $oid ei ole oikeuksia koulutustyyppiin $koulutustyyppi",
      id = "tarjoajaOidWoRequiredKoulutustyyppi"
    )

  def tarjoajaOidWoRequiredOppilaitostyyppi(oid: OrganisaatioOid, oppilaitostyypit: Seq[String]): ErrorMessage =
    ErrorMessage(
      msg = s"Tarjoaja $oid ei ole sallittua oppilaitostyyppiä (${oppilaitostyypit.mkString(", ")})",
      id = "tarjoajaOidWoRequiredOppilaitostyyppi"
    )
  def invalidEPerusteId(ePerusteId: Long): ErrorMessage = ErrorMessage(
    msg = s"EPerustetta id:llä $ePerusteId ei löydy, tai EPeruste ei ole voimassa",
    id = "invalidEPerusteId"
  )
  def invalidEPerusteIdForKoulutusKoodiUri(ePerusteId: Long, koodiUri: String): ErrorMessage =
    ErrorMessage(
      msg = s"Valittu koulutuskoodiuri ($koodiUri) ei ole hyväksytty EPerusteelle $ePerusteId",
      id = "invalidEPerusteIdForKoulutus"
    )
  def invalidTutkinnonOsaViiteForEPeruste(ePerusteId: Long, tutkinnonOsaViite: Long): ErrorMessage =
    ErrorMessage(
      msg = s"Tutkinnonosa-viite $tutkinnonOsaViite ei ole ole hyväksytty EPerusteelle $ePerusteId",
      id = "invalidTukinnonosaViiteForEPeruste"
    )
  def invalidTutkinnonOsaIdForEPeruste(ePerusteId: Long, tutkinnonOsaId: Long): ErrorMessage =
    ErrorMessage(
      msg = s"TutkinnonosaID $tutkinnonOsaId ei ole ole hyväksytty EPerusteelle $ePerusteId",
      id = "invalidTukinnonosaIdForEPeruste"
    )
  def invalidOsaamisalaForEPeruste(ePerusteId: Long, osaamisalaKoodiUri: String): ErrorMessage =
    ErrorMessage(
      msg = s"Osaamisala $osaamisalaKoodiUri ei ole ole hyväksytty EPerusteelle $ePerusteId",
      id = "invalidOsaamisalaForEPeruste"
    )
  def invalidTutkintoNimikeKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Tutkintonimike-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidTutkintoNimikeKoodiuri"
  )
  def invalidOpintojenLaajuusKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Opintojenlaajuus-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidOpintojenLaajuusKoodiuri"
  )
  def invalidOpetusKieliKoodiUri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Opetuskieli-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    "invalidOpetusKieliKoodiUri"
  )
  def invalidOpetusAikaKoodiUri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Opetusaika-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    "invalidOpetusAikaKoodiUri"
  )
  def invalidOpetusTapaKoodiUri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Opetustapa-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    "invalidOpetusTapaKoodiUri"
  )
  def invalidOsaamisalaKoodiUri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Osaamisala-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    "invalidOsaamisalaKoodiUri"
  )
  def invalidLukioDiplomiKoodiUri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Diplomi-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    "invalidLukioDiplomiKoodiUri"
  )
  def invalidKausiKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Opetuksen koulutuksenAlkamiskausi-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidKausiKoodiuri"
  )
  def invalidHakukohdeKoodiuri(koodiUri: String, koodisto: String): ErrorMessage = ErrorMessage(
    msg = s"Hakukohde-koodiuria $koodiUri ei löydy koodistosta $koodisto, tai ei ole voimassa",
    id = "invalidHakukohdeKoodiuri"
  )
  def invalidPohjakoulutusVaatimusKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Pohjakoulutusvaatimus-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidPohjakoulutusVaatimusKoodiuri"
  )
  def invalidLiitetyyppiKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Liitetyyppi-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidLiitetyyppiKoodiuri"
  )
  def invalidValintakoeTyyppiKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Valintakoetyyppi-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidValintakoeTyyppiKoodiuri"
  )
  def invalidOppiaineKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Oppiaine-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidOppiaineKoodiuri"
  )
  def invalidOppiaineKieliKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Oppiainekieli-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidOppiaineKieliKoodiuri"
  )
  def unknownValintaperusteenValintakoeIdForHakukohde(valintaperusteId: UUID, valintakoeId: UUID): ErrorMessage =
    ErrorMessage(
      msg = s"Valintakoetta ID:llä $valintakoeId ei löydy hakukohteen valintaperusteelle $valintaperusteId",
      id = "unknownValintaperusteenValintakoeIdForHakukohde"
    )
  def invalidPostiosoiteKoodiUri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Postiosoite-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidPostiosoiteKoodiUri"
  )
  def invalidSomeKoodiUri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Sosiaalinenmedia-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidSomeKoodiUri"
  )
  def unknownAtaruId(ataruId: UUID): ErrorMessage = ErrorMessage(
    msg = s"Hakulomaketta ID:llä $ataruId ei löydy, tai se on poistettu tai lukittu",
    id = "invalidAtaruId"
  )
  def invalidAtaruFormAllowsOnlyYhteishaku(ataruId: UUID): ErrorMessage = ErrorMessage(
    msg = s"Hakulokame ID:llä $ataruId sallii vain yhteishaut",
    id = "invalidAtaruFormAllowsOnlyYhteishaku"
  )
  def invalidHakutapaKoodiUri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Hakutapa-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidHakutapaKoodiUri"
  )
  def invalidHaunKohdejoukkoKoodiUri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Kohdejoukko-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidKohdejoukkoKoodiUri"
  )
  def invalidHaunKohdejoukonTarkenneKoodiUri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Haun kohdejoukon tarkenne-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidHaunKohdejoukonTarkenneKoodiUri"
  )
  def invalidValintatapaKoodiUri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Valintatapa-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidValintatapaKoodiUri"
  )
  def invalidTietoaOpiskelustaOtsikkoKoodiUri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Tietoa opiskelusta -otsikon koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidTietoaOpiskelustaOtsikkoKoodiUri"
  )
  def invalidTaiteenPerusopetusTaiteenalaKoodiuri(koodiUri: String): ErrorMessage = ErrorMessage(
    msg = s"Taiteen perusopetuksen taiteenala-koodiuria $koodiUri ei löydy, tai ei ole voimassa",
    id = "invalidTaiteenPerusopetusTaiteenalaKoodiuri"
  )
  def invalidJarjestyspaikkaOid(oid: OrganisaatioOid, toteutusOid: ToteutusOid): ErrorMessage = ErrorMessage(
    msg =
      s"JärjestyspaikkaOID:ia ${oid.s} ei löydy toteutuksen ${toteutusOid.s} tarjoajista eikä niiden ali-organisaatioista",
    id = "invalidJarjestyspaikkaOid"
  )
  def invalidHakukohteenLinja(linja: String): ErrorMessage = ErrorMessage(
    msg =
      s"Hakukohteen linja $linja ei ole sallittu. Linjan täytyy olla tyhjä (jolloin kyseessä yleislinja), tai vastata toteutuksen lukiopainotuksia tai erityis-koulutustehtäviä",
    id = "invalidHakukohteenLinja"
  )
  def invalidKoulutustyyppiForHakukohdeJarjestaaUrheilijanAmmKoulutusta(
      koulutustyyppi: Option[Koulutustyyppi]
  ): ErrorMessage = ErrorMessage(
    msg =
      s"Hakukohde saa järjestää ammatillista urheiljan koulutusta vain jos koulutuksen koulutustyyppi on Amm. Hakukohteen koulutustyyppi on ${koulutustyyppi.getOrElse("").toString}",
    id = "invalidKoulutustyyppiForHakukohdeJarjestaaUrheilijanAmmKoulutusta"
  )
  def invalidJarjestypaikkaForHakukohdeJarjestaaUrheilijanAmmKoulutusta(
      jarjestyspaikkaJarjestaaUrheilijanAmmKoulutusta: Boolean
  ): ErrorMessage = ErrorMessage(
    msg =
      s"Hakukohde saa järjestää ammatillista urheiljan koulutusta vain jos järjestyspaikka saa järjestää ammatillista urheiljan koulutusta. Järjestyspaikan jarjestaaUrheilijanAmmKoulutusta on ${jarjestyspaikkaJarjestaaUrheilijanAmmKoulutusta.toString}",
    id = "invalidJarjestypaikkaForHakukohdeJarjestaaUrheilijanAmmKoulutusta"
  )
  def lessOrEqualMsg(value: Long, comparedValue: Long): ErrorMessage =
    ErrorMessage(msg = s"$value saa olla pienempi kuin $comparedValue", id = "lessOrEqualMsg")

  def notInTheRangeMsg(min: Option[Double], max: Option[Double], givenValue: Option[Double]): ErrorMessage =
    ErrorMessage(msg = s"$givenValue ei ole välillä ${min.getOrElse("")}–${max.getOrElse("")}", id = "notInTheRangeMsg")

  def invalidKielistetty(values: Seq[Kieli]): ErrorMessage = ErrorMessage(
    msg = s"Kielistetystä kentästä puuttuu arvo kielillä [${values.mkString(",")}]",
    id = "invalidKielistetty"
  )
  def notAllowedKielistetty(values: Seq[Kieli]): ErrorMessage = ErrorMessage(
    msg = s"Kielistetyssä kentässä ei ole sallittu arvoa kielillä [${values.mkString(",")}",
    id = "notAllowedKielistetty"
  )
  def invalidTutkintoonjohtavuus(tyyppi: String): ErrorMessage =
    ErrorMessage(msg = s"Koulutuksen tyypin $tyyppi pitäisi olla tutkintoon johtava", id = "invalidTutkintoonjohtavuus")
  def invalidUrl(url: String): ErrorMessage = ErrorMessage(msg = s"'$url' ei ole validi URL", id = "invalidUrl")
  def invalidEmail(email: String): ErrorMessage =
    ErrorMessage(msg = s"'$email' ei ole validi email", id = "invalidEmail")
  def invalidAjanjaksoMsg(ajanjakso: Ajanjakso): ErrorMessage =
    ErrorMessage(msg = s"${ajanjakso.alkaa} - ${ajanjakso.paattyy} on virheellinen", id = "invalidAjanjaksoMsg")
  def pastDateMsg(date: LocalDateTime): ErrorMessage =
    ErrorMessage(msg = s"$date on menneisyydessä", id = "pastDateMsg")
  def pastDateMsg(date: String): ErrorMessage = ErrorMessage(msg = s"$date on menneisyydessä", id = "pastDateMsg")
  def minmaxMsg(minValue: Any, maxValue: Any): ErrorMessage =
    ErrorMessage(msg = s"$minValue on suurempi kuin $maxValue", id = "minmaxMsg")
  def notYetJulkaistu(field: String, id: Any): ErrorMessage =
    ErrorMessage(msg = s"$field ($id) ei ole vielä julkaistu", id = "notYetJulkaistu")
  def nonExistent(field: String, id: Any): ErrorMessage =
    ErrorMessage(msg = s"$field ($id) ei ole olemassa", id = "nonExistent")
  def notMissingMsg(value: Any): ErrorMessage =
    ErrorMessage(msg = s"Arvo $value ei saisi olla määritelty", id = "notMissingMsg")
  def tyyppiMismatch(field: String, id: Any): ErrorMessage =
    ErrorMessage(msg = s"Tyyppi ei vastaa $field ($id) tyyppiä", id = "tyyppiMismatch")
  def tyyppiMismatch(field1: String, id1: Any, field2: String, id2: Any): ErrorMessage =
    ErrorMessage(msg = s"$field1 ($id1) tyyppi ei vastaa $field2 ($id2) tyyppiä", id = "tyyppiMismatch")
  def cannotLinkToHakukohde(oid: String): ErrorMessage =
    ErrorMessage(msg = s"Toteutusta ($oid) ei voi liittää hakukohteeseen", id = "cannotLinkToHakukohde")
  def valuesDontMatch(relatedEntity: String, field: String): ErrorMessage =
    ErrorMessage(msg = s"$relatedEntity kenttä $field ei sisällä samoja arvoja", id = "valuesDontMatch")
  def oneNotBoth(field1: String, field2: String): ErrorMessage =
    ErrorMessage(msg = s"Tarvitaan joko $field1 tai $field2, mutta ei molempia.", id = "oneNotBoth")
  def noneOrOneNotBoth(field1: String, field2: String): ErrorMessage =
    ErrorMessage(msg = s"Voidaan valita joko $field1 tai $field2, mutta ei molempia.", id = "noneOrOneNotBoth")
  def notEmptyAlthoughOtherEmptyMsg(otherField: String): ErrorMessage =
    ErrorMessage(
      msg = s"Ei saa sisältää arvoa, koska kentässä $otherField ei ole arvoa",
      id = "notEmptyAlthoughOtherEmptyMsg"
    )
  def notEmptyAlthoughBooleanFalseMsg(booleanField: String): ErrorMessage =
    ErrorMessage(
      msg = s"Ei saa sisältää arvoa, koska $booleanField ei ole asetettu",
      id = "notEmptyAlthoughOtherEmptyMsg"
    )
  def notAllowedDueTo(reason: String): ErrorMessage =
    ErrorMessage(
      msg = s"Ei saa sisältää arvoa, koska $reason",
      id = "notAllowedDueTo"
    )
  def illegalStateChange(entityDesc: String, oldState: Julkaisutila, newState: Julkaisutila): ErrorMessage =
    ErrorMessage(
      msg =
        s"Siirtyminen tilasta ${Julkaisutila.getDisplauName(oldState)} (tilan tekninen tunniste: $oldState) tilaan ${Julkaisutila
          .getDisplauName(newState)} (tilan tekninen tunniste: $newState) ei ole sallittu $entityDesc",
      id = "illegalStateChange"
    )
  def illegalValueForFixedValueMsg(fixedValDesc: String): ErrorMessage = ErrorMessage(
    msg = s"Kentän täytyy sisältää tietty arvo: $fixedValDesc. Ko. arvo asetetaan automaattisesti jos kenttä on tyhjä",
    id = "illegalValueForFixedValueSeq"
  )
  def illegalValueForFixedValueSeqMsg(fixedValDesc: String): ErrorMessage = ErrorMessage(
    msg =
      s"Kentän täytyy sisältää täsmälleen yksi arvo: $fixedValDesc. Ko. arvo asetetaan automaattisesti jos kenttä on tyhjä",
    id = "illegalValueForFixedValueSeq"
  )
  def illegalNameForFixedlyNamedEntityMsg(expectedValue: String, nameSourceDesc: String): ErrorMessage = ErrorMessage(
    msg =
      s"Nimen täytyy olla '$expectedValue', vastaava kuin $nameSourceDesc. Nimi asetetaan automaattisesti jos kenttä on tyhjä.",
    id = "illegalNameForFixedlyNamedEntity"
  )
  def nameNotAllowedForFixedlyNamedEntityMsg(nameSourceDesc: String): ErrorMessage = ErrorMessage(
    msg = s"Ei saa sisältää arvoa, vastaavaa kielistettyä arvoa ei ole asetettu $nameSourceDesc",
    id = "nameNotAllowedForFixedlyNamedEntity"
  )
  def illegalOpintojenLaajuusNumero(expectedValue: Double): ErrorMessage = ErrorMessage(
    msg =
      s"Opintojen laajuuden numeroarvon täytyy olla täsmälleen $expectedValue. Arvo asetetaan automaattisesti jos kenttä on tyhjä",
    id = "illegalOpintojenLaajuusNumero"
  )
  def integrityViolationMsg(entityDesc: String, relatedEntity: String): ErrorMessage =
    ErrorMessage(msg = s"$entityDesc ei voi poistaa koska siihen on liitetty $relatedEntity", id = "integrityViolation")

  def invalidArkistointiDate(months: Int): ErrorMessage =
    ErrorMessage(
      msg =
        s"Arkistointipäivämäärän tulee olla vähintään $months kuukautta haun viimeisimmästä päättymispäivämäärästä.",
      id = "invalidArkistointiDate"
    )
  def unknownLiiteId(liiteId: String): ErrorMessage =
    ErrorMessage(msg = s"Liitettä ID:llä $liiteId ei löydy", id = "unknownLiiteId")

  def unknownValintakoeId(valintaKoeId: String): ErrorMessage =
    ErrorMessage(msg = s"Valintakoetta ID:llä $valintaKoeId ei löydy", id = "unknownValintakoeId")

  val koodistoServiceFailureMsg: ErrorMessage =
    ErrorMessage(
      msg = s"KoodiUrin voimassaoloa ei voitu tarkistaa, Koodisto-palvelussa tapahtui virhe. Yritä myöhemmin uudelleen",
      id = "koodistoServiceFailure"
    )

  val ataruServiceFailureMsg: ErrorMessage =
    ErrorMessage(
      msg =
        s"Hakemuslomakkeen voimassaoloa ei voitu tarkistaa, Ataru-palvelussa tapahtui virhe. Yritä myöhemmin uudelleen",
      id = "ataruServiceFailure"
    )

  val kaksoistutkintoValidationFailedDuetoKoodistoFailureMsg: ErrorMessage =
    ErrorMessage(
      msg =
        s"Kaksoistutkintoon liittyvien koulutus-koodiurien voimassaoloa ei voitu tarkistaa, Koodisto-palvelussa tapahtui virhe. Yritä myöhemmin uudelleen",
      id = "kaksoistutkintoValidationFailedDuetoKoodistoFailure"
    )

  val ePerusteServiceFailureMsg: ErrorMessage =
    ErrorMessage(
      msg =
        s"EPerusteen oikeellisuutta ei voitu tarkistaa, ePeruste-palvelussa tapahtui virhe. Yritä myöhemmin uudelleen",
      id = "ePerusteServiceFailure"
    )

  val organisaatioServiceFailureMsg: ErrorMessage =
    ErrorMessage(
      msg =
        s"Organisaatioiden tietoja ei voitu tarkistaa, Organisaatiopalvelussa tapahtui virhe. Yritä myöhemmin uudelleen",
      id = "organisaatioServiceFailure"
    )

  val lokalisointiServiceFailureMsg: ErrorMessage =
    ErrorMessage(
      msg = "Käännöksiä ei voitu hakea, Lokalisointipalvelussa tapahtui virhe. Yritä myöhemmin uudelleen",
      id = "lokalisointiServiceFailureMsg"
    )
  def uuidToString(uuid: Option[UUID]): String = uuid.map(_.toString).getOrElse("")

  val InvalidKoulutuspaivamaarat: ErrorMessage = ErrorMessage(
    msg = "koulutuksenAlkamispaivamaara tai koulutuksenPaattymispaivamaara on virheellinen",
    id = "InvalidKoulutuspaivamaarat"
  )
  val InvalidMetadataTyyppi: ErrorMessage =
    ErrorMessage(msg = "Koulutustyyppi ei vastaa metadatan tyyppiä", id = "InvalidMetadataTyyppi")

  val invalidOpetuskieliWithLukuvuosimaksu: ErrorMessage =
    ErrorMessage(
      msg = s"Lukuvuosimaksua ei voi määritellä, jos englantia ei ole valittu opetuskieleksi",
      id = "invalidOpetuskieliWithLukuvuosimaksu"
    )

  def invalidKoulutusWithLukuvuosimaksu(koulutuskoodiuri: Seq[String]): ErrorMessage =
    ErrorMessage(
      msg = s"Lukuvuosimaksua ei voi määritellä seuraavalle koulutukselle: $koulutuskoodiuri",
      id = "invalidKoulutusWithLukuvuosimaksu"
    )

  def invalidKoulutustyyppiWithLukuvuosimaksuMsg(koulutustyyppi: Koulutustyyppi): ErrorMessage =
    ErrorMessage(
      msg = s"Lukuvuosimaksua ei voi määritellä valitulle koulutustyypille: $koulutustyyppi",
      id = "invalidKoulutustyyppiWithLukuvuosimaksu"
    )

  val invalidMaksullisuustyyppiWithApuraha: ErrorMessage =
    ErrorMessage(
      msg = s"Apurahan voi asettaa vain toteutuksille, joille on asetettu lukuvuosimaksu",
      id = "invalidMaksullisuustyyppiWithApuraha"
    )
  val missingTarjoajatForNonJulkinenKoulutus: ErrorMessage =
    ErrorMessage(
      msg = "Tämän tyyppiselle koulutukselle täytyy valita vähintään yksi järjestäjä, ellei koulutus ole julkinen",
      id = "missingTarjoajatForNonJulkinenKoulutus"
    )
  def notModifiableMsg(parameter: String, entityType: String): ErrorMessage =
    ErrorMessage(msg = s"$parameter ei voi muuttaa olemassaolevalle $entityType", id = "notModifiable")

  val KoulutusKoodiPattern: Pattern                 = Pattern.compile("""koulutus_\d{6}#\d{1,2}""")
  val TietoaOpiskelustaOtsikkoKoodiPattern: Pattern = Pattern.compile("""organisaationkuvaustiedot_\d+#\d{1,2}""")
  val PostinumeroKoodiPattern: Pattern              = Pattern.compile("""posti_\d{5}(#\d{1,2})?""")

  val VuosiPattern: Pattern = Pattern.compile("""\d{4}""")

  val validStateChanges: Map[Julkaisutila, Seq[Julkaisutila]] =
    Map(
      Poistettu   -> Seq(),
      Tallennettu -> Seq(Julkaistu, Poistettu),
      Julkaistu   -> Seq(Tallennettu, Arkistoitu),
      Arkistoitu  -> Seq(Julkaistu)
    )
  def assertTrue(b: Boolean, path: String, msg: ErrorMessage): IsValid  = if (b) NoErrors else error(path, msg)
  def assertFalse(b: Boolean, path: String, msg: ErrorMessage): IsValid = if (!b) NoErrors else error(path, msg)
  def assertNotNegative(i: Long, path: String): IsValid                 = assertTrue(i >= 0, path, notNegativeMsg)
  def assertNotNegative(i: Double, path: String): IsValid               = assertTrue(i >= 0, path, notNegativeMsg)
  def assertLessOrEqual(i: Int, x: Int, path: String): IsValid          = assertTrue(i <= x, path, lessOrEqualMsg(i, x))
  def assertMatch(value: String, pattern: Pattern, path: String): IsValid =
    assertTrue(pattern.matcher(value).matches(), path, validationMsg(value))
  def assertValid(oid: Oid, path: String): IsValid                  = assertTrue(oid.isValid, path, validationMsg(oid.toString))
  def assertNotOptional[T](value: Option[T], path: String): IsValid = assertTrue(value.isDefined, path, missingMsg)
  def assertNotEmpty[T](value: Seq[T], path: String): IsValid       = assertTrue(value.nonEmpty, path, missingMsg)
  def assertEmpty[T](value: Seq[T], path: String, errorMessage: ErrorMessage = notEmptyMsg): IsValid =
    assertTrue(value.isEmpty, path, errorMessage)
  def assertEmptyKielistetty(kielistetty: Kielistetty, path: String): IsValid =
    assertTrue(kielistetty.isEmpty, path, notEmptyMsg)

  def koodiUriTipText(koodiUri: String): String =
    s"$koodiUri#<versionumero>, esim. $koodiUri#1"

  def assertCertainValue(
      value: Option[String],
      expectedValuePrefix: String,
      path: String
  ): IsValid =
    assertTrue(
      value.isDefined && value.get.startsWith(expectedValuePrefix),
      path,
      illegalValueForFixedValueMsg(koodiUriTipText(expectedValuePrefix))
    )

  def assertOneAndOnlyCertainValueInSeq(
      value: Seq[String],
      expectedValuePrefix: String,
      path: String
  ): IsValid =
    if (value.size == 1 && value.head.startsWith(expectedValuePrefix)) {
      NoErrors
    } else {
      error(path, illegalValueForFixedValueSeqMsg(koodiUriTipText(expectedValuePrefix)))
    }

  def assertNotDefined[T](value: Option[T], path: String): IsValid =
    assertTrue(value.isEmpty, path, notMissingMsg(value))
  def assertNotDefinedIfOtherNotDefined[A, B](
      value: Option[A],
      other: Option[B],
      otherField: String,
      path: String
  ): IsValid =
    if (other.isEmpty && value.nonEmpty) error(path, notEmptyAlthoughOtherEmptyMsg(otherField)) else NoErrors
  def assertAlkamisvuosiInFuture(alkamisvuosi: String, path: String): IsValid =
    assertTrue(LocalDate.now().getYear <= Integer.parseInt(alkamisvuosi), path, pastDateMsg(alkamisvuosi))

  def assertValidUrl(url: String, path: String): IsValid = assertTrue(urlValidator.isValid(url), path, invalidUrl(url))
  def assertValidEmail(email: String, path: String): IsValid =
    assertTrue(emailValidator.isValid(email), path, invalidEmail(email))

  def assertInFuture(date: LocalDateTime, path: String): IsValid =
    assertTrue(date.isAfter(LocalDateTime.now()), path, pastDateMsg(date))

  def assertNimiMatchExternal(
      nimi: Kielistetty,
      nimiFromExternalSource: Kielistetty,
      path: String,
      externalSourceDesc: String
  ): IsValid = {
    val nameNotAllowedLngs =
      nimi.keySet.filter(lng => !nimiFromExternalSource.contains(lng) && nimi(lng) != null && nimi(lng).nonEmpty)
    val nameNotMatchingLngs =
      nimi.keySet.filter(lng => nimiFromExternalSource.contains(lng) && nimi(lng) != nimiFromExternalSource(lng))
    nameNotAllowedLngs
      .map(lng => ValidationError(s"$path.${lng.name}", nameNotAllowedForFixedlyNamedEntityMsg(externalSourceDesc)))
      .toList ++
      nameNotMatchingLngs
        .map(lng =>
          ValidationError(
            s"$path.${lng.name}",
            illegalNameForFixedlyNamedEntityMsg(nimiFromExternalSource(lng), externalSourceDesc)
          )
        )
        .toList
  }

  def assertKoodistoQueryResult(
      koodiUri: String,
      queryMethod: String => ExternalQueryResult,
      path: String,
      validationContext: ValidationContext,
      errorMessage: ErrorMessage
  ): IsValid = {
    val queryResult = if (validationContext.isKoodistoServiceOk()) queryMethod(koodiUri) else queryFailed
    validationContext.updateKoodistoServiceStatusByQueryStatus(queryResult)
    assertExternalQueryResult(
      queryResult,
      path,
      errorMessage,
      koodistoServiceFailureMsg
    )
  }

  def assertKoulutuskoodiQueryResult(
      koulutusKoodiUri: String,
      koulutusKoodiFilter: KoulutusKoodiFilter,
      koodistoService: KoodistoService,
      path: String,
      validationContext: ValidationContext,
      errorMessage: ErrorMessage,
      externalServiceFailureMessage: ErrorMessage = koodistoServiceFailureMsg
  ): IsValid = {
    val queryResult =
      if (validationContext.isKoodistoServiceOk()) {
        if (koulutusKoodiFilter.filterType() == koulutusTyyppi) {
          koodistoService.isInLisattavatKoulutukset(
            koulutusKoodiFilter.koulutusTyypit,
            koulutusKoodiUri
          )
        } else
          koodistoService.isLisattavaKoulutus(koulutusKoodiFilter.koulutusKoodiUrit, koulutusKoodiUri)
      } else queryFailed
    validationContext.updateKoodistoServiceStatusByQueryStatus(queryResult)
    assertExternalQueryResult(
      queryResult,
      path,
      errorMessage,
      externalServiceFailureMessage
    )
  }

  def assertAtaruQueryResult(
      ataruId: UUID,
      hakemusPalveluClient: HakemusPalveluClient,
      path: String
  ): IsValid = {
    assertExternalQueryResult(
      hakemusPalveluClient.isExistingAtaruIdFromCache(ataruId),
      path,
      unknownAtaruId(ataruId),
      ataruServiceFailureMsg
    )
  }

  def assertAtaruFormAllowsOnlyYhteisHakuResult(
      ataruId: UUID,
      hakutapa: Option[String],
      hakemusPalveluClient: HakemusPalveluClient,
      path: String,
      errorMessage: ErrorMessage
  ): IsValid = {
    assertExternalQueryResult(
      hakemusPalveluClient.isFormAllowedForHakutapa(ataruId, hakutapa),
      path,
      errorMessage,
      ataruServiceFailureMsg
    )
  }

  def assertExternalQueryResult(
      externalQueryResult: ExternalQueryResult,
      path: String,
      errorMessage: ErrorMessage,
      externalServiceFailureMessage: ErrorMessage
  ): IsValid =
    externalQueryResult match {
      case e if e == itemFound   => NoErrors
      case e if e == queryFailed => error(path, externalServiceFailureMessage)
      case _                     => error(path, errorMessage)
    }

  def validateIfDefined[T](value: Option[T], f: T => IsValid): IsValid = value.map(f(_)).getOrElse(NoErrors)

  def validateIfDefinedAndTrue(value: Option[Boolean], f: IsValid): IsValid =
    value.map(validateIfTrue(_, f)).getOrElse(NoErrors)

  def validateIfDefinedOrModified[T](value: Option[T], oldValue: Option[T], f: T => IsValid): IsValid =
    (value, oldValue) match {
      case (Some(value), Some(oldValue)) => if (value != oldValue) f(value) else NoErrors
      case (Some(value), None)           => f(value)
      case _                             => NoErrors
    }

  def validateIfNonEmpty[T](values: Seq[T], path: String, f: (T, String) => IsValid): IsValid =
    values.zipWithIndex.flatMap { case (t, i) => f(t, s"$path[$i]") }

  def validateIfNonEmptySeq[T](
      values: Seq[T],
      newValues: Seq[T],
      path: String,
      f: (T, Option[T], String) => IsValid
  ): IsValid =
    values.zipWithIndex.flatMap { case (t, i) =>
      if (values.size == newValues.size) f(t, newValues.lift(i), s"$path[$i]") else f(t, None, s"$path[$i]")
    }

  def validateIfNonEmpty(k: Kielistetty, path: String, f: (String, String) => IsValid): IsValid =
    k.flatMap { case (k, v) => f(v, s"$path.$k") }.toSeq

  def validateIfTrue(b: Boolean, f: => IsValid): IsValid                      = if (b) f else NoErrors
  def validateIfTrueOrElse(b: Boolean, f: => IsValid, o: => IsValid): IsValid = if (b) f else o
  def validateIfFalse(b: Boolean, f: => IsValid): IsValid                     = if (!b) f else NoErrors

  def validateIfJulkaistu(tila: Julkaisutila, f: => IsValid): IsValid    = validateIfTrue(tila == Julkaistu, f)
  def validateIfAnyDefined(args: Seq[Option[_]], f: => IsValid): IsValid = validateIfTrue(args.exists(_.isDefined), f)
  def validateIfAnyDefinedOrElse(args: Seq[Option[_]], f: => IsValid, o: => IsValid): IsValid =
    if (args.exists(_.isDefined)) f else o
  def validateIfSuccessful(success: IsValid, f: => IsValid): IsValid = if (success.isEmpty) f else success

  def validateOidList(values: Seq[Oid], path: String): IsValid = validateIfNonEmpty(values, path, assertValid _)

  def findMissingKielet(kielivalinta: Seq[Kieli], k: Kielistetty): Seq[Kieli] = {
    kielivalinta.diff(k.keySet.toSeq).union(k.filter { case (_, arvo) => arvo == null || arvo.isEmpty }.keySet.toSeq)
  }

  def findNonAllowedKielet(kielivalinta: Seq[Kieli], k: Kielistetty): Seq[Kieli] =
    k.keySet.filter(lng => !kielivalinta.contains(lng) && k(lng) != null && k(lng).nonEmpty).toSeq

  def validateKielistetty(kielivalinta: Seq[Kieli], k: Kielistetty, path: String): IsValid = {
    val missing = findMissingKielet(kielivalinta, k) match {
      case x if x.isEmpty => NoErrors
      case kielet         => error(path, invalidKielistetty(kielet))
    }
    val notAllowed = findNonAllowedKielet(kielivalinta, k) match {
      case x if x.isEmpty => NoErrors
      case kielet         => error(path, notAllowedKielistetty(kielet))
    }
    missing ++ notAllowed
  }

  def validateOptionalKielistetty(kielivalinta: Seq[Kieli], k: Kielistetty, path: String): IsValid =
    validateIfTrue(k.values.exists(_.nonEmpty), validateKielistetty(kielivalinta, k, path))

  def validateHakulomake(
      hakulomaketyyppi: Option[Hakulomaketyyppi],
      hakulomakeAtaruId: Option[UUID],
      hakulomakeKuvaus: Kielistetty,
      hakulomakeLinkki: Kielistetty,
      kielivalinta: Seq[Kieli]
  ): IsValid = hakulomaketyyppi match {
    case Some(MuuHakulomake) =>
      and(
        assertNotDefined(hakulomakeAtaruId, "hakulomakeAtaruId"),
        assertEmptyKielistetty(hakulomakeKuvaus, "hakulomakeKuvaus"),
        validateKielistetty(kielivalinta, hakulomakeLinkki, "hakulomakeLinkki"),
        hakulomakeLinkki.flatMap { case (_, u) => assertValidUrl(u, "hakulomakeLinkki") }.toSeq
      )
    case Some(Ataru) =>
      and(
        assertNotOptional(hakulomakeAtaruId, "hakulomakeAtaruId"),
        assertEmptyKielistetty(hakulomakeKuvaus, "hakulomakeKuvaus"),
        assertEmptyKielistetty(hakulomakeLinkki, "hakulomakeLinkki")
      )
    case Some(EiSähköistä) =>
      and(
        assertNotDefined(hakulomakeAtaruId, "hakulomakeAtaruId"),
        assertEmptyKielistetty(hakulomakeLinkki, "hakulomakeLinkki"),
        validateOptionalKielistetty(kielivalinta, hakulomakeKuvaus, "hakulomakeKuvaus")
      )
    case _ =>
      and(
        assertNotDefinedIfOtherNotDefined(hakulomakeAtaruId, hakulomaketyyppi, "hakulomaketyyppi", "hakulomakeAtaruId"),
        assertTrue(hakulomakeKuvaus.isEmpty, "hakulomakeKuvaus", notEmptyAlthoughOtherEmptyMsg("hakulomaketyyppi")),
        assertTrue(hakulomakeLinkki.isEmpty, "hakulomakeLinkki", notEmptyAlthoughOtherEmptyMsg("hakulomaketyyppi"))
      )
  }

  def validateArkistointiPaivamaara(
      ajastettuHaunJaHakukohteidenArkistointi: Option[LocalDateTime],
      haunPaattymisPaivamaarat: List[Option[LocalDateTime]]
  ): IsValid = {
    val arkistointiAikaisintaanKuukautta = 3
    ajastettuHaunJaHakukohteidenArkistointi match {
      case Some(pvm) =>
        haunPaattymisPaivamaarat.flatten.sortWith(_.isBefore(_)) match {
          case paattymisPaivamaarat
              if paattymisPaivamaarat.nonEmpty && paattymisPaivamaarat.last.toLocalDate.until(
                pvm.toLocalDate,
                ChronoUnit.MONTHS
              ) < arkistointiAikaisintaanKuukautta =>
            error("ajastettuHaunJaHakukohteidenArkistointi", invalidArkistointiDate(arkistointiAikaisintaanKuukautta))
          case _ => NoErrors
        }
      case _ => NoErrors
    }
  }

  def validateKoulutusPaivamaarat(
      koulutuksenAlkamispaivamaara: Option[LocalDateTime],
      koulutuksenPaattymispaivamaara: Option[LocalDateTime],
      alkamisPath: String
  ): IsValid = {
    koulutuksenAlkamispaivamaara
      .flatMap(alku =>
        koulutuksenPaattymispaivamaara.map(loppu =>
          assertTrue(alku.isBefore(loppu), alkamisPath, InvalidKoulutuspaivamaarat)
        )
      )
      .getOrElse(NoErrors)
  }

  def validateMinMax[T](min: Option[T], max: Option[T], minPath: String)(implicit n: Numeric[T]): IsValid =
    (min, max) match {
      case (Some(min), Some(max)) => assertTrue(n.toDouble(min) <= n.toDouble(max), minPath, minmaxMsg(min, max))
      case _                      => NoErrors
    }

  def validateDependency(
      validatableTila: Julkaisutila,
      dependencyTila: Option[Julkaisutila],
      dependencyId: Any,
      dependencyName: String,
      dependencyIdPath: String
  ): IsValid = {
    dependencyTila.map { tila =>
      and(
        assertTrue(tila != Poistettu, path = dependencyIdPath, nonExistent(dependencyName, dependencyId)),
        validateIfTrue(
          tila != Poistettu,
          validateIfJulkaistu(
            validatableTila,
            assertTrue(tila == Julkaistu, "tila", notYetJulkaistu(dependencyName, dependencyId))
          )
        )
      )
    }.getOrElse(error(dependencyIdPath, Validations.nonExistent(dependencyName, dependencyId)))
  }

  def validateDependencyExistence(
      dependencyTila: Option[Julkaisutila],
      dependencyId: Any,
      dependencyName: String,
      dependencyIdPath: String
  ): IsValid = {
    dependencyTila.map { tila =>
      assertTrue(tila != Poistettu, path = dependencyIdPath, nonExistent(dependencyName, dependencyId))
    }.getOrElse(error(dependencyIdPath, nonExistent(dependencyName, dependencyId)))
  }

  def validateStateChange(entityDesc: String, oldState: Option[Julkaisutila], newState: Julkaisutila): IsValid = {
    validateIfDefinedAndTrue(
      oldState.map(_ != newState),
      validateIfDefined[Seq[Julkaisutila]](
        oldState.flatMap(validStateChanges.get(_)),
        validStates =>
          assertTrue(validStates.contains(newState), "tila", illegalStateChange(entityDesc, oldState.get, newState))
      )
    )
  }

  def validateSubEntityId(
      subEntityId: Option[UUID],
      path: String,
      crudOperation: CrudOperation,
      allowedIds: Seq[UUID],
      notAllowedMsg: ErrorMessage
  ): IsValid =
    validateIfTrueOrElse(
      crudOperation == update,
      assertTrue(!subEntityId.isDefined || allowedIds.contains(subEntityId.get), path, notAllowedMsg),
      assertNotDefined(subEntityId, path)
    )

  def assertKoulutusKoodiuriAmount(koodiUrit: Seq[String], maxNbrOfKoodit: Option[Int]): IsValid = and(
    assertNotEmpty(koodiUrit, "koulutuksetKoodiUri"),
    validateIfDefined[Int](
      maxNbrOfKoodit,
      nbr => assertTrue(koodiUrit.size <= nbr, "koulutuksetKoodiUri", tooManyKoodiUris)
    )
  )

  def invalidKielistettyByOtherFields(kielet: Seq[Kieli], otherPaths: Seq[String]): ErrorMessage = ErrorMessage(
    msg = s"Kielistetystä kentästä puuttuu muissa kentissä määriteltyjä arvoja kielillä [${kielet
      .mkString(",")}]. Muut kentät ovat [${otherPaths.mkString(",")}]",
    id = "invalidKielistettyByOtherFields"
  )

  def assertKielistettyHasLocalesRequiredByOtherFields(
      kaikkiKielet: Seq[Kieli],
      k: Kielistetty,
      path: String,
      allPaths: Seq[String]
  ): IsValid = {
    val otherPaths = allPaths.filter(!path.equals(_))
    findMissingKielet(kaikkiKielet, k) match {
      case x if x.isEmpty => NoErrors
      case kielet         => error(path, invalidKielistettyByOtherFields(kielet, otherPaths))
    }

  }
  def assertKielistetytHavingSameLocales(kielistetyt: (Kielistetty, String)*): IsValid = {
    val kaikkiKielet: Seq[Kieli] = kielistetyt
      .map(_._1)
      .flatMap(kielistetty =>
        kielistetty.filter(kielistetty => kielistetty._2 != null && kielistetty._2.nonEmpty).keySet
      )
      .distinct
    val allPaths: Seq[String] = kielistetyt.map(_._2)
    and(
      kielistetyt.map { case (kielistetty, path) =>
        assertKielistettyHasLocalesRequiredByOtherFields(kaikkiKielet, kielistetty, path, allPaths)
      }: _*
    )
  }

  def validateTeemakuvaUrl(teemakuva: Option[String], imageBucketUrl: String): IsValid = {
    val isTest = imageBucketUrl.contains(".untuvaopintopolku.fi");
    val urls = if (isTest) List(imageBucketUrl, "https://konfo-files.opintopolku.fi") else List(imageBucketUrl)

    validateIfDefined[String](
      teemakuva,
      tk => {
        val urlValidationErrors = assertValidUrl(tk, "teemakuva")
        if (urlValidationErrors.nonEmpty) {
          urlValidationErrors
        } else {
          assertTrue(
            urls.exists(tk.startsWith),
            "teemakuva",
            ErrorMessage("Teemakuvalla on väärä domain", "invalidUrlDomain")
          )
        }
      }
    )
  }

  private lazy val imageBucketPublicUrl = KoutaConfigurationFactory.configuration.s3Configuration.imageBucketPublicUrl

  def validateTeemakuvaWithConfig(teemakuva: Option[String]): IsValid =
    validateTeemakuvaUrl(teemakuva, imageBucketPublicUrl)
}
