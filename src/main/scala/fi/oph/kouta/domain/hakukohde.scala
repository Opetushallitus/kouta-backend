package fi.oph.kouta.domain

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.validation.{IsValid, Validatable}

case class Hakukohde(oid:Option[String] = None,
                     toteutusOid:String,
                     hakuOid:String,
                     tila:Julkaisutila = Tallennettu,
                     nimi: Kielistetty = Map(),
                     alkamiskausiKoodiUri: Option[String] = None,
                     alkamisvuosi: Option[String] = None,
                     hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                     hakulomake: Option[String] = None,
                     aloituspaikat: Option[Int] = None,
                     ensikertalaisenAloituspaikat: Option[Int] = None,
                     pohjakoulutusvaatimusKoodiUri: Option[String] = None,
                     muuPohjakoulutusvaatimus: Kielistetty = Map(),
                     toinenAsteOnkoKaksoistutkinto: Option[Boolean] = None,
                     kaytetaanHaunAikataulua: Option[Boolean] = None,
                     valintaperuste: Option[UUID] = None,
                     liitteetOnkoSamaToimitusaika: Option[Boolean] = None,
                     liitteetOnkoSamaToimitusosoite: Option[Boolean] = None,
                     liitteidenPalautusaika: Option[Instant] = None,
                     liitteidenToimitustapa: Option[LiitteenToimitustapa] = None,
                     liitteidenToimitusosoite: Option[LiitteenToimitusosoite] = None,
                     liitteet: List[Liite] = List(),
                     valintakokeet: List[Valintakoe] = List(),
                     hakuajat: List[Ajanjakso] = List(),
                     muokkaaja:String,
                     kielivalinta:Seq[Kieli] = Seq()) extends PerustiedotWithOid with Validatable {

  override def validate(): IsValid = for {
    _ <- super.validate().right
    _ <- validateHakukohdeOid(oid)
    _ <- validateToteutusOid(toteutusOid)
    _ <- validateHakuOid(hakuOid)
    x <- validateIfTrue(tila == Julkaistu, () => for {
      _ <- validateKausiKoodi(alkamiskausiKoodiUri).right
      _ <- validatePohjakoulutusvaatimusKoodi(pohjakoulutusvaatimusKoodiUri).right
      _ <- validateAlkamisvuosi(alkamisvuosi).right
      _ <- validateHakulomake(hakulomaketyyppi, hakulomake).right
      y <- validateHakuajat(hakuajat).right
    } yield y).right
  } yield x
}

case class Valintakoe(id:Option[UUID] = None,
                      tyyppi:Option[String] = None,
                      tilaisuudet:List[Valintakoetilaisuus])

case class Valintakoetilaisuus(osoite:Option[Osoite],
                               aika:Option[Ajanjakso] = None,
                               lisatietoja:Kielistetty = Map())

case class Liite(id:Option[UUID] = None,
                 tyyppi:Option[String],
                 nimi: Kielistetty = Map(),
                 kuvaus: Kielistetty = Map(),
                 palautusaika: Option[Instant] = None,
                 toimitustapa: Option[LiitteenToimitustapa] = None,
                 toimitusosoite: Option[LiitteenToimitusosoite] = None)

case class LiitteenToimitusosoite(osoite:Osoite,
                                  sahkoposti:Option[String] = None)