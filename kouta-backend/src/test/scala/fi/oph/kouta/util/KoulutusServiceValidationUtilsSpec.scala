package fi.oph.kouta.util

import fi.oph.kouta.TestOids.{AmkOid, ChildOid, GrandChildOid, HkiYoOid, OphOid, ParentOid, YoOid}
import fi.oph.kouta.domain.oid.OrganisaatioOid

class KoulutusServiceValidationUtilsSpec extends UnitSpec {
  "getUnremovableTarjoajat" should "return empty list if no tarjoajat removed" in {
    val removedTarjoajatWithOids = Map[OrganisaatioOid, Seq[OrganisaatioOid]]()
    val toteutustenTarjoajat = Seq(YoOid)
    assert(KoulutusServiceValidationUtil.getUnremovableTarjoajat(removedTarjoajatWithOids, toteutustenTarjoajat) === List())
  }

  it should "return empty List if removedTarjoaja is not attached to a toteutus" in {
    val removedTarjoajatWithOids = Map(HkiYoOid -> Seq(HkiYoOid, OphOid))
    val toteutustenTarjoajat = Seq(YoOid)
    assert(KoulutusServiceValidationUtil.getUnremovableTarjoajat(removedTarjoajatWithOids, toteutustenTarjoajat) === List())
  }

  it should "return one organisaatioOid in unremovable tarjoajat list when toteutus has it attached as a tarjoaja" in {
    val removedTarjoajatWithOids = Map(HkiYoOid -> Seq(HkiYoOid, OphOid))
    val toteutustenTarjoajat = Seq(HkiYoOid)
    assert(KoulutusServiceValidationUtil.getUnremovableTarjoajat(removedTarjoajatWithOids, toteutustenTarjoajat) === List(HkiYoOid))
  }

  it should "return two organisaatioOids in unremovable tarjoajat list when toteutus has both of them attached as tarjoajat" in {
    val removedTarjoajatWithOids = Map(HkiYoOid -> Seq(HkiYoOid, OphOid), GrandChildOid -> Seq(OphOid, ParentOid, GrandChildOid, ChildOid))
    val toteutustenTarjoajat = Seq(HkiYoOid, ChildOid)
    assert(KoulutusServiceValidationUtil.getUnremovableTarjoajat(removedTarjoajatWithOids, toteutustenTarjoajat) === List(HkiYoOid, GrandChildOid))
  }
}
