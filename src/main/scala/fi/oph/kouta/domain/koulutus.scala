package fi.oph.kouta.domain

import java.time.LocalDateTime

import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, UserOid}
import fi.oph.kouta.validation.Validatable

case class KoulutusMetadata(kuvaus: Map[Kieli, String] = Map(),
                            lisatiedot: Seq[Lisatieto] = Seq(),
                            kuvauksenNimi: Map[Kieli, String] = Map(),
                            tutkintonimikeKoodiUrit: Seq[String] = Seq(),
                            opintojenLaajuusKoodiUri: Option[String] = None
                           )

case class Koulutus(oid: Option[KoulutusOid] = None,
                    johtaaTutkintoon: Boolean,
                    koulutustyyppi: Option[Koulutustyyppi] = None,
                    koulutusKoodiUri: Option[String] = None,
                    tila: Julkaisutila = Tallennettu,
                    tarjoajat: List[OrganisaatioOid] = List(),
                    nimi:  Kielistetty = Map(),
                    metadata: Option[KoulutusMetadata] = None,
                    julkinen: Boolean = false,
                    muokkaaja: UserOid,
                    organisaatioOid: OrganisaatioOid,
                    kielivalinta: Seq[Kieli] = Seq(),
                    modified: Option[LocalDateTime]) extends PerustiedotWithOid with Validatable {

  override def validate() = {
    and(super.validate(),
        validateIfDefined[KoulutusOid](oid, assertValid(_)),
        validateOidList(tarjoajat),
        validateIfDefined[String](koulutusKoodiUri, assertMatch(_, KoulutusKoodiPattern)),
        validateIfTrue(Julkaistu == tila, () => and(
          assertNotOptional(koulutustyyppi, "koulutustyyppi"),
          validateIfDefined[Koulutustyyppi](koulutustyyppi, (k) => assertTrue(k == Muu | johtaaTutkintoon, invalidTutkintoonjohtavuus(k.toString))),
          assertNotOptional(koulutusKoodiUri, "koulutusKoodiUri"))))
  }
}

case class KoulutusListItem(oid: KoulutusOid,
                            nimi: Kielistetty,
                            tila: Julkaisutila,
                            organisaatioOid: OrganisaatioOid,
                            muokkaaja: UserOid,
                            modified: LocalDateTime) extends OidListItem