package fi.oph.kouta.util

import java.net.InetAddress
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import org.json4s.JsonAST.JString
import org.json4s.ext.JavaTypesSerializers
import org.json4s.prefs.ExtractionNullStrategy
import org.json4s.jackson.Serialization.write
import org.json4s.{CustomKeySerializer, CustomSerializer, DefaultFormats, Formats, JNull, MappingException, Serialization}

import scala.util.control.NonFatal

trait GenericKoutaJsonFormats extends GenericKoutaFormats {
  implicit def jsonFormats: Formats = genericKoutaFormats

  def toJson(data: AnyRef): String = write(data)
}

trait GenericKoutaFormats {

  val ISO_LOCAL_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
  val ISO_MODIFIED_FORMATTER: DateTimeFormatter        = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

  def genericKoutaFormats: Formats = withKoutaSerializers(DefaultFormats.strict)

  /**
   * Formaatit indeksistä luettaville dokumenteille.
   *
   * Json4s 4.0 tiukensi kahta asiaa, jotka 3.6 hyväksyi:
   *  - uusi strictMapExtraction-lippu (mukana DefaultFormats.strictissä) hylkää literaalin nullin
   *    Map- tai Kielistetty-kentässä; 3.6 palautti Map.empty
   *  - strictOptionParsing sai uuden tarkistuksen ("No value set for Option properties"), joka
   *    hylkää case classin, jonka kaikki Option-kentät ovat asettamatta
   * Kouta-indeksoijan kirjoittamissa dokumenteissa esim. haun metadata.koulutuksenAlkamiskausi voi
   * puuttua kokonaan tai olla null, ja tällainen dokumentti katosi hakutuloksista päivityksen
   * jälkeen. Luetaan indeksidokumentit siksi 3.6:n tapaan sallivasti - kokoelmien tiukkuus
   * (strictArrayExtraction) säilyy. Sisääntulevan JSONin validointi pysyy ennallaan, koska se
   * käyttää genericKoutaFormatsia.
   *
   * TreatAsAbsent tarvitaan, jotta literaali null käsitellään puuttuvana kenttänä ja case classin
   * oletusarvo kelpaa. Oletuksena (Keep) nullista tulisi Scala-null keskelle valmista objektia, ja
   * dokumentti hajoaisi vasta myöhemmin NullPointerExceptioniin.
   */
  def indexedDocumentFormats: Formats = withKoutaSerializers(
    DefaultFormats.withStrictArrayExtraction.withExtractionNullStrategy(ExtractionNullStrategy.TreatAsAbsent)
  )

  private def withKoutaSerializers(base: Formats): Formats = base
    .addKeySerializers(Seq(kieliKeySerializer)) ++ JavaTypesSerializers.all ++
    Seq(
      LocalDateTimeSerializer,
      ModifiedSerializer,
      stringSerializer(Julkaisutila.withName),
      stringSerializer(Koulutustyyppi.withName),
      stringSerializer(Hakulomaketyyppi.withName),
      stringSerializer(Hakutermi.withName),
      stringSerializer(Apurahayksikko.withName),
      stringSerializer(Maksullisuustyyppi.withName),
      stringSerializer(Alkamiskausityyppi.withName),
      stringSerializer(Kieli.withName),
      stringSerializer(LiitteenToimitustapa.withName),
      stringSerializer(HakuOid),
      stringSerializer(HakukohdeOid),
      stringSerializer(HakukohderyhmaOid),
      stringSerializer(KoulutusOid),
      stringSerializer(ToteutusOid),
      stringSerializer(OrganisaatioOid),
      stringSerializer(UserOid),
      stringSerializer(GenericOid),
      stringSerializer(InetAddress.getByName, (ip: InetAddress) => ip.getHostAddress),
    )

  case object LocalDateTimeSerializer extends CustomSerializer[LocalDateTime](_ => ( {
    case JString(i) =>
      try {
        LocalDateTime.from(ISO_LOCAL_DATE_TIME_FORMATTER.parse(i))
      } catch {
        case NonFatal(e) =>
          throw new MappingException(e.getMessage, new java.lang.IllegalArgumentException(e))
      }
    case JNull => null
  }, {
    case i: LocalDateTime => JString(ISO_LOCAL_DATE_TIME_FORMATTER.format(i))
  }))

  case object ModifiedSerializer extends CustomSerializer[Modified](_ => ( {
    case JString(i) =>
      try {
        Modified(LocalDateTime.from(ISO_MODIFIED_FORMATTER.parse(i)))
      } catch {
        case NonFatal(e) =>
          throw new MappingException(e.getMessage, new java.lang.IllegalArgumentException(e))
      }
    case JNull => null
  }, {
    case i: Modified => JString(ISO_MODIFIED_FORMATTER.format(i.value))
  }))

  private def kieliKeySerializer = new CustomKeySerializer[Kieli](_ => ( {
    case s: String => Kieli.withName(s)
  }, {
    case k: Kieli => k.toString
  }))

  private def stringSerializer[A>:Null: Manifest](construct: String => A): CustomSerializer[A] =
    stringSerializer(construct, (a: A) => a.toString)

  private def stringSerializer[A>:Null: Manifest](construct: String => A, deconstruct: A => String) =
    new CustomSerializer[A](_ => ( {
      case JString(s) => 
        try {
          construct(s)
        } catch {
          case NonFatal(e) =>
            throw new MappingException(e.getMessage, new java.lang.IllegalArgumentException(e))
        }
      case JNull => null
    }, {
      case a: A => JString(deconstruct(a))
    }))
}
