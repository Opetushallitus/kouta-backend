package fi.oph.kouta.domain

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.validation.{IsValid, Validatable}

import scala.language.implicitConversions

case class Valintaperuste(id: Option[UUID] = None,
                          tila: Julkaisutila = Tallennettu,
                          hakutapaKoodiUri: Option[String] = None,
                          kohdejoukkoKoodiUri: Option[String] = None,
                          kohdejoukonTarkenneKoodiUri: Option[String] = None,
                          nimi: Kielistetty = Map(),
                          onkoJulkinen: Boolean = false,
                          metadata: Option[ValintaperusteMetadata] = None,
                          organisaatioOid: OrganisaatioOid,
                          muokkaaja: UserOid,
                          kielivalinta: Seq[Kieli] = Seq(),
                          modified: Option[LocalDateTime]) extends PerustiedotWithId with Validatable {

  override def validate(): IsValid = and(
     super.validate(),
     validateIfDefined[String](hakutapaKoodiUri, assertMatch(_, HakutapaKoodiPattern)),
     validateIfDefined[String](kohdejoukkoKoodiUri, assertMatch(_, KohdejoukkoKoodiPattern)),
     validateIfDefined[String](kohdejoukonTarkenneKoodiUri, assertMatch(_, KohdejoukonTarkenneKoodiPattern)),
     validateIfTrue(Julkaistu == tila, () => and(
       assertNotOptional(hakutapaKoodiUri, "hakutapaKoodiUri"),
       assertNotOptional(kohdejoukkoKoodiUri, "kohdejoukkoKoodiUri")
     ))
  )
}

sealed trait ValintatapaSisaltoData
object ValintatapaSisaltoData {
  implicit def data2Sisalto(data: ValintatapaSisaltoData): ValintatapaSisalto = data match {
    case teksti: ValintatapaSisaltoTeksti => ValintatapaSisalto("teksti", teksti)
    case taulukko: Taulukko => ValintatapaSisalto("taulukko", taulukko)
  }
}

sealed case class ValintatapaSisalto(tyyppi: String, data: ValintatapaSisaltoData)


case class Taulukko(id: Option[UUID],
                    nimi: Kielistetty = Map(),
                    rows: Seq[Row] = Seq()) extends ValintatapaSisaltoData

case class ValintatapaSisaltoTeksti(teksti: String) extends ValintatapaSisaltoData

case class Row(index: Int,
               isHeader: Boolean = false,
               columns: Seq[Column] = Seq())

case class Column(index: Int,
                  text: Kielistetty = Map())

case class Valintatapa(nimi: Kielistetty = Map(),
                       valintatapaKoodiUri: Option[String] = None,
                       kuvaus: Kielistetty = Map(),
                       sisalto: Seq[ValintatapaSisalto],
                       kaytaMuuntotaulukkoa: Boolean = false,
                       kynnysehto: Kielistetty = Map(),
                       enimmaispisteet: Option[Double] = None,
                       vahimmaispisteet: Option[Double] = None ) extends Validatable {
  override def validate(): IsValid = and(
    validateIfDefined[String](valintatapaKoodiUri, assertMatch(_, ValintatapajonoKoodiPattern))
  )
}

case class Kielitaito(kielitaitoKoodiUri: Option[String] = None,
                      lisatieto: Kielistetty = Map())

case class KielitaitovaatimusKuvaus(kielitaitovaatimusKuvausKoodiUri: Option[String] = None,
                                    kielitaitovaatimusTaso: Option[String] = None)

case class Kielitaitovaatimus(kielitaitovaatimusKoodiUri: Option[String] = None,
                              kielitaitovaatimusKuvaukset: Seq[KielitaitovaatimusKuvaus] = Seq())

case class ValintaperusteKielitaitovaatimus(kieliKoodiUri: Option[String] = None,
                                            kielitaidonVoiOsoittaa: Seq[Kielitaito] = Seq(),
                                            vaatimukset: Seq[Kielitaitovaatimus] = Seq())

case class ValintaperusteMetadata(valintatavat: Seq[Valintatapa],
                                  kielitaitovaatimukset: Seq[ValintaperusteKielitaitovaatimus],
                                  osaamistaustaKoodiUrit: Seq[String] = Seq(),
                                  kuvaus: Kielistetty = Map(),
                                 )

case class ValintaperusteListItem(id: UUID,
                                  nimi: Kielistetty,
                                  tila: Julkaisutila,
                                  organisaatioOid: OrganisaatioOid,
                                  muokkaaja: UserOid,
                                  modified: LocalDateTime) extends IdListItem
