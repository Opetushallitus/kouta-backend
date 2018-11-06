package fi.oph.kouta.validation

import fi.oph.kouta.domain._

trait Validations extends ValidationMessages {

  def assertTrue(b:Boolean, msg:String): IsValid = Either.cond(b, (), msg)
  def assertOption[E](o:Option[E], f:(E) => Boolean, msg:String, optional:Boolean = true): IsValid = assertTrue(o.map(f).getOrElse(optional), msg)
  def assertOptionPresent[E](o:Option[E], msg:String): IsValid = assertTrue(o.isDefined, msg)

  def findInvalidOids(l:Seq[String]): Seq[String] = l.filter(!OidValidator.isOid(_))

  def findPuuttuvatKielet(kielivalinta:Seq[Kieli], k:Kielistetty):Seq[Kieli] = {
    kielivalinta.diff(k.keySet.toSeq).union(
      k.filter{case (kieli, arvo) => arvo.isEmpty}.keySet.toSeq
    )
  }

  def validateKielistetty(kielivalinta:Seq[Kieli], k:Kielistetty, msg:String): IsValid =
    findPuuttuvatKielet(kielivalinta, k) match {
      case x if !x.isEmpty => Left(invalidKielistetty(msg, x))
      case _ => Right()
    }

  def validateKoulutusKoodi(koulutusKoodiUri:Option[String]): IsValid = koulutusKoodiUri match {
    case None => Left(MissingKoulutuskoodi)
    case Some(x) if !KoodiValidator.isKoulutusKoodi(x) => Left(invalidKoulutuskoodi(x))
    case _ => Right()
  }

  def validateTarjoajat(tarjoajat:Seq[String]): IsValid = findInvalidOids(tarjoajat) match {
    case x if !x.isEmpty => Left(invalidTarjoajaOids(x))
    case _ => Right()
  }

  def validateKoulutusOid(oid:String): IsValid = assertTrue(
    OidValidator.isKoulutusOid(oid), invalidKoulutusOidMsg(oid))

  def validateHakuOid(oid:String): IsValid = assertTrue(
    OidValidator.isHakuOid(oid), invalidHakuOidMsg(oid))

  def validateKoulutusKoodi(k:String): IsValid =
    assertTrue(KoodiValidator.isKoulutusKoodi(k), invalidKoulutuskoodi(k))

  def validateHakutapaKoodi(k:Option[String], optional:Boolean = false): IsValid =
    assertOption(k, KoodiValidator.isHakutapaKoodi, invalidHakutapaKoodi(k.getOrElse("")), optional)

  def validateKausiKoodi(k:Option[String], optional:Boolean = false): IsValid =
    assertOption(k, KoodiValidator.isKausiKoodi, invalidKausiKoodi(k.getOrElse("")), optional)

  def validateKohdejoukkoKoodi(k:Option[String], optional:Boolean = false): IsValid =
    assertOption(k, KoodiValidator.isKohdejoukkoKoodi, invalidKohdejoukkoKoodi(k.getOrElse("")), optional)

  def validateKohdejoukonTarkenneKoodi(k:Option[String], optional:Boolean = false): IsValid =
    assertOption(k, KoodiValidator.isKohdejoukonTarkenneKoodi, invalidKohdejoukonTarkenneKoodi(k.getOrElse("")), optional)

  def validatePohjakoulutusvaatimusKoodi(k:Option[String], optional:Boolean = false): IsValid =
    assertOption(k, KoodiValidator.isPohjakoulutusvaatimusKoodi, invalidPohjakoulutusvaatimusKoodi(k.getOrElse("")), optional)

  def validateKielivalinta(kielivalinta:Seq[Kieli]): IsValid =
    assertTrue(kielivalinta.size > 0, MissingKielivalinta)

  def validateOid(oid:String): IsValid = assertTrue(
    OidValidator.isOid(oid), invalidOidMsg(oid))

  def validateOid(oid:Option[String]): IsValid = assertOption(
    oid, OidValidator.isOid(_), invalidOidMsg(oid.getOrElse("")))

  def validateKoulutusOid(oid:Option[String]): IsValid = assertOption(
    oid, OidValidator.isKoulutusOid(_), invalidKoulutusOidMsg(oid.getOrElse("")))

  def validateToteutusOid(oid:Option[String]): IsValid = assertOption(
    oid, OidValidator.isToteutusOid(_), invalidToteutusOidMsg(oid.getOrElse("")))

  def validateHakukohdeOid(oid:Option[String]): IsValid = assertOption(
    oid, OidValidator.isHakukohdeOid(_), invalidHakukohdeOidMsg(oid.getOrElse("")))

  def validateHakuOid(oid:Option[String]): IsValid = assertOption(
    oid, OidValidator.isHakuOid(_), invalidHakuOidMsg(oid.getOrElse("")))

  def validateMuokkaaja(muokkaaja:String): IsValid = assertTrue(
    OidValidator.isOid(muokkaaja), invalidOidMsg(muokkaaja))

  def validateAlkamisvuosi(alkamisvuosi:Option[String]): IsValid = assertOption(
    alkamisvuosi, TimeValidator.isValidAlkamisvuosi(_), invalidAlkamisvuosi(alkamisvuosi.getOrElse("")), true)

  def validateHakulomake(hakulomaketyyppi:Option[Hakulomaketyyppi], hakulomake:Option[String]): IsValid =
    assertOptionPresent(hakulomaketyyppi, MissingHakulomaketyyppi)

  def validateHakuajat(hakuajat: List[Ajanjakso]): IsValid = hakuajat.filterNot(TimeValidator.isValidHakuaika) match {
    case x if x.isEmpty => Right()
    case x => Left(InvalidHakuaika)
  }

  def validateTutkintoonjohtavuus(tyyppi:Koulutustyyppi, johtaaTutkintoon:Boolean): IsValid = assertTrue(
    tyyppi == Muu || johtaaTutkintoon, invalidTutkintoonjohtavuus(tyyppi.toString))

  def validateIfTrue(b:Boolean, f:() => IsValid): IsValid = b match {
    case true => f()
    case _ => Right()
  }

  def validateKoulutustyyppi(koulutustyyppi:Option[Koulutustyyppi]): IsValid =
    assertOptionPresent(koulutustyyppi, MissingKoulutustyyppi)
}

trait ValidationMessages {
  def invalidKoulutusOidMsg(oid:String) = s"${oid} ei ole validi koulutuksen oid"
  def invalidToteutusOidMsg(oid:String) = s"${oid} ei ole validi toteutuksen oid"
  def invalidHakukohdeOidMsg(oid:String) = s"${oid} ei ole validi hakukohteen oid"
  def invalidHakuOidMsg(oid:String) = s"${oid} ei ole validi haun oid"
  def invalidOidMsg(oid:String) = s"${oid} ei ole validi oid"

  def invalidKielistetty(field:String, values:Seq[Kieli]) = s"Kielistetystä kentästä $field puuttuu arvo kielillä [${values.mkString(",")}]"
  def invalidTutkintoonjohtavuus(tyyppi:String) = s"Koulutuksen tyyppiä ${tyyppi} pitäisi olla tutkintoon johtavaa"
  def invalidTarjoajaOids(oids:Seq[String]) = s"Tarjoaja oidit [${oids.mkString(",")}] eivät ole valideja oideja"
  def invalidAlkamisvuosi(vuosi:String) = s"Alkamisvuosi $vuosi on virheellinen"

  def invalidKoulutuskoodi(koodi:String) = s"${koodi} ei ole validi koulutuskoodi"
  def invalidHakutapaKoodi(koodi:String) = s"${koodi} ei ole validi hakutapakoodi"
  def invalidKausiKoodi(koodi:String) = s"${koodi} ei ole validi kausikoodi"
  def invalidKohdejoukkoKoodi(koodi:String) = s"${koodi} ei ole validi kohdejoukkokoodi"
  def invalidKohdejoukonTarkenneKoodi(koodi:String) = s"${koodi} ei ole validi kohdejoukon tarkenne -koodi"
  def invalidPohjakoulutusvaatimusKoodi(koodi:String) = s"${koodi} ei ole validi pohjakoulutusvaatimuskoodi"

  val MissingKielivalinta = "Kielivalinta puuttuu"
  val MissingKoulutustyyppi = "Koulutustyyppi puuttuu julkaistavalta koulutukselta"
  val MissingKoulutuskoodi = "Julkaistulla koulutuksella pitää olla koulutuskoodi"
  val KuvausNotAccepted = "Ammatillisella koulutuksella ei saa olla kuvausta"
  val MissingHakulomaketyyppi = "Hakulomaketyyppi puuttuu"
  val InvalidHakuaika = "Hakuaika on virheellinen"
}

