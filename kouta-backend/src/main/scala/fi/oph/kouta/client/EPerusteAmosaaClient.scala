package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.logging.Logging
import fi.vm.sade.properties.OphProperties
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import java.net.URLEncoder

case class EPerusteAmosaaQueryException(url: String, status: Int, message: String) extends RuntimeException(message)

case class AmosaaLokalisoituTeksti(fi: Option[String] = None, sv: Option[String] = None, en: Option[String] = None)

// Opetussuunnitelmat response

case class AmosaaKoulutus(
    id: Option[Long],
    nimi: Option[AmosaaLokalisoituTeksti],
    koulutuskoodiArvo: Option[String],
    koulutuskoodiUri: Option[String],
    koulutusalakoodi: Option[String],
    opintoalakoodi: Option[String]
)

case class AmosaaPeruste(
    id: Long,
    diaarinumero: Option[String],
    perusteId: Option[Long],
    nimi: Option[AmosaaLokalisoituTeksti],
    koulutustyyppi: Option[String],
    koulutukset: Option[List[AmosaaKoulutus]]
)

case class AmosaaKoulutustoimija(
    id: Long,
    organisaatio: String,
    nimi: Option[AmosaaLokalisoituTeksti],
    organisaatioRyhma: Boolean,
    oppilaitostyyppi: Option[String],
    oppilaitosTyyppiKoodiUri: Option[String]
)

case class AmosaaOpetussuunnitelma(
    id: Long,
    nimi: Option[AmosaaLokalisoituTeksti],
    tila: Option[String],
    tyyppi: Option[String],
    kuvaus: Option[String],
    koulutustoimija: Option[AmosaaKoulutustoimija],
    luotu: Option[Long],
    muokattu: Option[Long],
    peruste: Option[AmosaaPeruste],
    jotpatyyppi: Option[String],
    julkaisukielet: Option[List[String]],
    voimaantulo: Option[Long],
    voimassaoloLoppuu: Option[Long],
    esikatseltavissa: Option[Boolean],
    koulutustyyppi: Option[String],
    oppilaitosTyyppiKoodiUri: Option[String]
)

case class AmosaaOpetussuunnitelmatResponse(
    data: List[AmosaaOpetussuunnitelma],
    sivuja: Option[Int],
    `kokonaismäärä`: Option[Int],
    sivu: Option[Int],
    sivukoko: Option[Int]
)

// Paikalliset tutkinnonosat response

case class AmosaaVaatimus(koodi: Option[String], vaatimus: Option[AmosaaLokalisoituTeksti])

case class AmosaaKohdealue(kuvaus: Option[AmosaaLokalisoituTeksti], vaatimukset: Option[List[AmosaaVaatimus]])

case class AmosaaAmmattitaitovaatimukset(
    id: Long,
    kohde: Option[AmosaaLokalisoituTeksti],
    kohdealueet: Option[List[AmosaaKohdealue]],
    vaatimukset: Option[List[AmosaaVaatimus]]
)

case class AmosaaArviointi(
    id: Long,
    lisatiedot: Option[AmosaaLokalisoituTeksti],
    arvioinninkohdealueet: Option[List[AmosaaKohdealue]]
)

case class AmosaaKoodi(uri: String, arvo: String, nimi: Option[AmosaaLokalisoituTeksti], versio: Int, koodisto: String)

case class AmosaaOsaamistaso(id: Long, koodi: Option[AmosaaKoodi], otsikko: Option[AmosaaLokalisoituTeksti])

case class AmosaaOsaamistasonKriteeri(
    kriteerit: Option[List[AmosaaLokalisoituTeksti]],
    osaamistaso: Option[AmosaaOsaamistaso]
)

case class AmosaaArviointiasteikko(id: Long, osaamistasot: Option[List[AmosaaOsaamistaso]])

case class AmosaaGeneerinenArviointiasteikko(
    id: Long,
    nimi: Option[AmosaaLokalisoituTeksti],
    kohde: Option[AmosaaLokalisoituTeksti],
    arviointiasteikko: Option[AmosaaArviointiasteikko],
    osaamistasonkriteerit: Option[List[AmosaaOsaamistasonKriteeri]]
)

case class AmosaaOmaTutkinnonosa(
    id: Long,
    koodi: Option[String],
    laajuus: Option[BigDecimal],
    arviointi: Option[AmosaaArviointi],
    geneerinenarviointi: Option[Long],
    ammattitaitovaatimukset: Option[AmosaaAmmattitaitovaatimukset],
    geneerinenarviointiasteikko: Option[AmosaaGeneerinenArviointiasteikko],
    ammattitaidonosoittamistavat: Option[AmosaaLokalisoituTeksti],
    ammattitaitovaatimuksetlista: Option[AmosaaAmmattitaitovaatimukset]
)

case class AmosaaTosa(
    id: Long,
    tyyppi: String,
    muokattu: Option[Long],
    omatutkinnonosa: Option[AmosaaOmaTutkinnonosa]
)

case class AmosaaTekstiKappale(
    id: Long,
    nimi: Option[AmosaaLokalisoituTeksti],
    luotu: Option[Long],
    teksti: Option[AmosaaLokalisoituTeksti],
    muokattu: Option[Long],
    tunniste: Option[String]
)

case class AmosaaPaikallinenTutkinnonosa(
    id: Long,
    nimi: Option[AmosaaLokalisoituTeksti],
    tosa: Option[AmosaaTosa],
    tyyppi: Option[String],
    tekstikappale: Option[AmosaaTekstiKappale],
    lapset: Option[List[AmosaaPaikallinenTutkinnonosa]],
    naytapohjanteksti: Option[Boolean],
    naytaperusteenteksti: Option[Boolean]
)

object EPerusteAmosaaClient extends EPerusteAmosaaClient(KoutaConfigurationFactory.configuration.urlProperties)

class EPerusteAmosaaClient(urlProperties: OphProperties) extends HttpClient with CallerId with Logging {
  implicit val formats = DefaultFormats

  val errorHandler = (url: String, status: Int, response: String) =>
    throw EPerusteAmosaaQueryException(url, status, response)

  def getOpetussuunnitelmat(
      organisaatiot: Set[String],
      nimi: Option[String],
      paikallistaSisaltoa: Option[Boolean],
      sivu: String,
      sivukoko: String
  ): AmosaaOpetussuunnitelmatResponse = {
    def encodeParam(value: String): String = URLEncoder.encode(value, "UTF-8")

    val queryParams = organisaatiot.map(org => s"organisaatio=${encodeParam(org)}") ++ Seq(
      nimi.map(n => s"nimi=${encodeParam(n)}"),
      paikallistaSisaltoa.map(p => s"paikallistasisaltoa=${encodeParam(p.toString)}")
    ).flatten + s"sivu=$sivu" + s"sivukoko=$sivukoko"

    val url = urlProperties.url("eperusteet-amosaa-service.opetussuunnitelmat") +
      (if (queryParams.nonEmpty) s"?${queryParams.mkString("&")}" else "")
    get(url, errorHandler, followRedirects = true) { response =>
      parse(response).extract[AmosaaOpetussuunnitelmatResponse]
    }
  }

  def getOpetussuunnitelma(opetussuunnitelmaId: Long): AmosaaOpetussuunnitelma = {
    val url = urlProperties.url("eperusteet-amosaa-service.opetussuunnitelma", opetussuunnitelmaId.toString)
    get(url, errorHandler, followRedirects = true) { response =>
      parse(response).extract[AmosaaOpetussuunnitelma]
    }
  }

  def getPaikallisetTutkinnonosat(opetussuunnitelmaId: Long): List[AmosaaPaikallinenTutkinnonosa] = {
    val url = urlProperties.url(
      "eperusteet-amosaa-service.opetussuunnitelman-tutkinnonosat",
      opetussuunnitelmaId.toString
    ) + "?tosa.tyyppi=oma"
    get(url, errorHandler, followRedirects = true) { response =>
      parse(response).extract[List[AmosaaPaikallinenTutkinnonosa]]
    }
  }
}
