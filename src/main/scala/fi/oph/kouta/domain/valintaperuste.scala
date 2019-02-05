package fi.oph.kouta.domain

import java.util.UUID

import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.validation.{IsValid, Validatable}

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
                          kielivalinta: Seq[Kieli] = Seq()) extends PerustiedotWithId with Validatable {

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

case class Taulukko(id: Option[UUID],
                    nimi: Kielistetty = Map(),
                    rows: Seq[Row] = Seq())

case class Row(index: Int,
               isHeader: Boolean = false,
               columns: Seq[Column] = Seq())

case class Column(index: Int,
                  text: Kielistetty = Map())

case class Valintatapa(valintatapaKoodiUri: Option[String] = None,
                       kuvaus: Kielistetty = Map(),
                       taulukot: Seq[Taulukko],
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
                                  kielitaitovaatimukset: Seq[ValintaperusteKielitaitovaatimus])