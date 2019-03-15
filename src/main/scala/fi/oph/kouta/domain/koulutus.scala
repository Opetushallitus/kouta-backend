package fi.oph.kouta.domain

import java.time.LocalDateTime

import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, UserOid}
import fi.oph.kouta.validation.Validatable

sealed trait KoulutusMetadata {
  val kuvaus: Map[Kieli, String]
  val lisatiedot: Seq[Lisatieto]
}

trait KorkeakoulutusKoulutusMetadata extends KoulutusMetadata {
  val tutkintonimikeKoodiUrit: Seq[String]
  val opintojenLaajuusKoodiUri: Option[String]
}

case class AmmatillinenKoulutusMetadata(kuvaus: Map[Kieli, String] = Map(), lisatiedot: Seq[Lisatieto] = Seq()) extends KoulutusMetadata {
}

case class YliopistoKoulutusMetadata(kuvaus: Map[Kieli, String] = Map(), lisatiedot: Seq[Lisatieto] = Seq(), tutkintonimikeKoodiUrit: Seq[String] = Seq(), opintojenLaajuusKoodiUri: Option[String] = None) extends KorkeakoulutusKoulutusMetadata {
}

case class AmmattikorkeakouluKoulutusMetadata(kuvaus: Map[Kieli, String] = Map(), lisatiedot: Seq[Lisatieto] = Seq(), tutkintonimikeKoodiUrit: Seq[String] = Seq(), opintojenLaajuusKoodiUri: Option[String] = None) extends KorkeakoulutusKoulutusMetadata {
}

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