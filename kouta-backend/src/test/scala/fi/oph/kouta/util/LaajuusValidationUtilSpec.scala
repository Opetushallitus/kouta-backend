package fi.oph.kouta.util

class LaajuusValidationUtilSpec extends UnitSpec {
  "isValidOpintojenLaajuus" should "return true for kk-opintokokonaisuus with laajuus in the defined range" in {
    assert(LaajuusValidationUtil.isValidOpintojenLaajuus(Some(5), Some(15), Some(10)) == true)
  }

  it should "return true if opintojenlaajuus is the same as min value" in {
    assert(LaajuusValidationUtil.isValidOpintojenLaajuus(Some(5), Some(15), Some(5)) == true)
  }

  it should "return true if opintojenlaajuus is the same as max value" in {
    assert(LaajuusValidationUtil.isValidOpintojenLaajuus(Some(5), Some(15), Some(15)) == true)
  }

  it should "return true if opintojenlaajuus is equal to min value and max value is not defined" in {
    assert(LaajuusValidationUtil.isValidOpintojenLaajuus(Some(5), None, Some(5)) == true)
  }

  it should "return true if opintojenlaajuus bigger than min value and max value is not defined" in {
    assert(LaajuusValidationUtil.isValidOpintojenLaajuus(Some(5), None, Some(6)) == true)
  }

  it should "return true if opintojenlaajuus is bigger than min value and max value is not defined" in {
    assert(LaajuusValidationUtil.isValidOpintojenLaajuus(Some(5), None, Some(6)) == true)
  }

  it should "return true if opintojenlaajuus is smaller than max value and min value is not defined" in {
    assert(LaajuusValidationUtil.isValidOpintojenLaajuus(None, Some(15), Some(14)) == true)
  }

  it should "return true if min and max values are not defined" in {
    assert(LaajuusValidationUtil.isValidOpintojenLaajuus(None, None, Some(6)) == true)
  }

  it should "return true if laajuus, min and max are the same" in {
    assert(LaajuusValidationUtil.isValidOpintojenLaajuus(Some(5), Some(5), Some(5)) == true)
  }

  it should "return true if toteutus laajuus is not defined" in {
    assert(LaajuusValidationUtil.isValidOpintojenLaajuus(Some(5), Some(15), None) == true)
  }

  it should "return false if laajuus smaller than min and max also defined" in {
    assert(LaajuusValidationUtil.isValidOpintojenLaajuus(Some(5), Some(15), Some(4)) == false)
  }

  it should "return false if laajuus smaller than min and max not defined" in {
    assert(LaajuusValidationUtil.isValidOpintojenLaajuus(Some(5), None, Some(4)) == false)
  }

  it should "return false if laajuus bigger than max and min also defined" in {
    assert(LaajuusValidationUtil.isValidOpintojenLaajuus(Some(5), Some(15), Some(16)) == false)
  }

  it should "return false if laajuus bigger than max and min not defined" in {
    assert(LaajuusValidationUtil.isValidOpintojenLaajuus(None, Some(15), Some(16)) == false)
  }

}
