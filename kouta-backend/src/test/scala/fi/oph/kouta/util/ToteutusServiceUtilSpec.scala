package fi.oph.kouta.util

class ToteutusServiceUtilSpec extends UnitSpec {
  "validateOpintojenlaajuus" should "succeed for kk-opintokokonaisuus with laajuus in the defined range" in {
    assert(ToteutusServiceUtil.isValidOpintojenlaajuus(Some(5), Some(15), Some(10)) == true)
  }

  "validateOpintojenlaajuus" should "succeed if opintojenlaajuus is the same as min value" in {
    assert(ToteutusServiceUtil.isValidOpintojenlaajuus(Some(5), Some(15), Some(5)) == true)
  }

  "validateOpintojenlaajuus" should "succeed if opintojenlaajuus is the same as max value" in {
    assert(ToteutusServiceUtil.isValidOpintojenlaajuus(Some(5), Some(15), Some(15)) == true)
  }

  "validateOpintojenlaajuus" should "succeed if opintojenlaajuus is the same as min value and max value is not defined" in {
    assert(ToteutusServiceUtil.isValidOpintojenlaajuus(Some(5), None, Some(5)) == true)
  }

  "validateOpintojenlaajuus" should "succeed if opintojenlaajuus is the same as max value and min value is not defined" in {
    assert(ToteutusServiceUtil.isValidOpintojenlaajuus(None, Some(5), Some(5)) == true)
  }

  "validateOpintojenlaajuus" should "fail if opintojenlaajuus is different from min value and max value is not defined" in {
    assert(ToteutusServiceUtil.isValidOpintojenlaajuus(Some(5), None, Some(6)) == false)
  }

  "validateOpintojenlaajuus" should "fail if opintojenlaajuus is different from max value and min value is not defined" in {
    assert(ToteutusServiceUtil.isValidOpintojenlaajuus(None, Some(15), Some(6)) == false)
  }

  "validateOpintojenlaajuus" should "succeed if min and max values are not defined" in {
    assert(ToteutusServiceUtil.isValidOpintojenlaajuus(None, None, Some(6)) == true)
  }
}
