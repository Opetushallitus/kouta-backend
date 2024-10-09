package fi.oph.kouta.validation

import fi.oph.kouta.TestData._
import fi.oph.kouta.domain.oid.{KoulutusOid, ToteutusOid}
import fi.oph.kouta.domain.{Julkaistu, MinimalExistingToteutus, Modified, Tallennettu}
import fi.oph.kouta.service.validation.LiitettyEntityValidation
import fi.oph.kouta.util.UnitSpec

import java.time.LocalDateTime

class LiitettyEntityValidationSpec extends UnitSpec {
  val toteutusOid = Some(ToteutusOid("1.2.246.562.17.00000000000000000124"))
  val toteutusOid2 = Some(ToteutusOid("1.2.246.562.17.00000000000000000125"))
  val koulutusOid1 = KoulutusOid("1.2.246.562.13.00000000000000000997")
  val julkaistuVstOsaamismerkkiKoulutus = VapaaSivistystyoOsaamismerkkiKoulutus.copy(
    oid = Some(koulutusOid1))
  val julkaistuVstToteutus = MinimalExistingToteutus(VapaaSivistystyoOpistovuosiToteutus.copy(
    oid = toteutusOid,
    koulutusOid = KoulutusOid("1.2.246.562.13.139"),
    tila = Julkaistu,
    modified = Some(Modified(LocalDateTime.now().minusDays(1))),
    metadata = Some(VapaaSivistystyoOpistovuosiToteutusMetatieto.copy(liitetytOsaamismerkit = List(koulutusOid1)))))

  "validateLiitettyEntityIntegrity" should "fail if liitetty osaamismerkki is changed from julkaistu to tallennettu when it is attached to a julkaistu osaamismerkki" in {
    val julkaistutToteutukset = Vector(julkaistuVstToteutus)
    assert(LiitettyEntityValidation.validateLiitettyEntityIntegrity(
      julkaistuVstOsaamismerkkiKoulutus.copy(tila = Tallennettu), Vector(julkaistuVstToteutus)) ==
      List(ValidationError(
        path = "metadata.tila",
        msg = "Entiteetin 1.2.246.562.13.00000000000000000997 tilaa ei voi vaihtaa, koska se on liitetty entiteetteihin List(Some(1.2.246.562.17.00000000000000000124)), joiden tila on Julkaistu.",
        errorType =  "invalidStateChangeForLiitetty",
        meta = Some(Map("julkaistutToteutukset" -> julkaistutToteutukset.map(_.oid)))))
    )
  }

  it should "succeed if liitetty osaamismerkki is changed from julkaistu to tallennettu when it is attached to a tallennettu osaamismerkki" in {
    val tallennettuVstToteutus = julkaistuVstToteutus.copy(tila = Tallennettu)
    assert(LiitettyEntityValidation.validateLiitettyEntityIntegrity(
      julkaistuVstOsaamismerkkiKoulutus.copy(tila = Tallennettu), Vector(tallennettuVstToteutus)) == List()
    )
  }

  val julkaistuKkOpintojaksoToteutus = JulkaistuKkOpintojaksoToteutus.copy(oid = toteutusOid)
  val julkaistuKkOpintokokonaisuusToteutus = MinimalExistingToteutus(JulkaistuKkOpintokokonaisuusToteutus.copy(
    oid = toteutusOid2,
    modified = Some(Modified(LocalDateTime.now().minusDays(1))),
    metadata = Some(KkOpintokokonaisuusToteutuksenMetatieto.copy(liitetytOpintojaksot = List(toteutusOid.get)))))

  it should "fail if liitetty opintojakso is changed from julkaistu to tallennettu when it is attached to a julkaistu opintokokonaisuus" in {
    val julkaistutToteutukset = Vector(julkaistuKkOpintokokonaisuusToteutus)
    assert(LiitettyEntityValidation.validateLiitettyEntityIntegrity(
      julkaistuKkOpintojaksoToteutus.copy(tila = Tallennettu), julkaistutToteutukset) ==
      List(ValidationError(
        path = "metadata.tila",
        msg = "Entiteetin 1.2.246.562.17.00000000000000000124 tilaa ei voi vaihtaa, koska se on liitetty entiteetteihin List(Some(1.2.246.562.17.00000000000000000125)), joiden tila on Julkaistu.",
        errorType = "invalidStateChangeForLiitetty",
        meta = Some(Map("julkaistutToteutukset" -> julkaistutToteutukset.map(_.oid)))))
    )
  }

  it should "succeed if liitetty opintojakso is changed from julkaistu to tallennettu when it is attached to a tallennettu opintokokonaisuus" in {
    val tallennettuKkOpintokokonaisuusToteutus = julkaistuKkOpintokokonaisuusToteutus.copy(tila = Tallennettu)
    assert(LiitettyEntityValidation.validateLiitettyEntityIntegrity(
      julkaistuKkOpintojaksoToteutus.copy(tila = Tallennettu), Vector(tallennettuKkOpintokokonaisuusToteutus)) ==
      List()
    )
  }
}
