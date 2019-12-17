package fi.oph.kouta.integration

import fi.oph.kouta.integration.fixture.MockS3Client.Content
import fi.oph.kouta.integration.fixture.{MockS3Client, UploadFixture}
import org.json4s.jackson.Serialization.read

case class ImageUrl(url: String)

class UploadSpec extends KoutaIntegrationSpec with UploadFixture {

  "Upload teemakuva" should "upload image" in {
    post(uri = TeemakuvaUploadPath, body = correctTeemakuva, headers = Map("Content-Type" -> "image/png")) {
      status should equal(200)
      read[ImageUrl](body).url match {
        case s3Service.tempUrl(filename) =>
          val content = MockS3Client.getLocal(ImageBucket, s3Service.getTempKey(filename))
          content should not be empty
          val Content(localData, metadata) = content.get
          localData should equal(correctTeemakuva)
          metadata.getCacheControl should equal("max-age=86400")
          metadata.getContentType should equal("image/png")
        case url => fail(s"$url was not an url to the public bucket")
      }
    }
  }

  it should "reject requests without a Content-Type header" in {
    post(uri = TeemakuvaUploadPath, body = correctTeemakuva, headers = None) {
      withClue(body) {
        status should equal(400)
      }
      body should include("Content-Type")
    }
  }

  it should "reject requests with an unsupported Content-Type header" in {
    post(uri = TeemakuvaUploadPath, body = correctTeemakuva, headers = Map("Content-Type" -> "image/bmp")) {
      withClue(body) {
        status should equal(415)
      }
      body should include("image/bmp")
    }
  }

  it should "reject too large images" in {
    val bigImage = new Array[Byte](MaxSizeInTest + 1)
    post(uri = TeemakuvaUploadPath, body = bigImage, headers = Map("Content-Type" -> "image/png")) {
      status should equal(413)
    }
  }

  it should "reject zero-size images" in {
    val zeroImage = new Array[Byte](0)
    post(uri = TeemakuvaUploadPath, body = zeroImage, headers = Map("Content-Type" -> "image/png")) {
      status should equal(400)
    }
  }

  it should "reject images that have too few pixels" in {
    post(uri = TeemakuvaUploadPath, body = tooSmallHeader, headers = Map("Content-Type" -> "image/png")) {
      status should equal(400)
      body should include("väärän kokoinen")
    }
  }

  it should "also accept jpeg images" in {
    post(uri = TeemakuvaUploadPath, body = correctJpgTeemakuva, headers = Map("Content-Type" -> "image/jpeg")) {
      status should equal(200)
      read[ImageUrl](body).url match {
        case s3Service.tempUrl(filename) =>
          val content = MockS3Client.getLocal(ImageBucket, s3Service.getTempKey(filename))
          content should not be empty
          val Content(localData, metadata) = content.get
          localData should equal(correctJpgTeemakuva)
          metadata.getCacheControl should equal("max-age=86400")
          metadata.getContentType should equal("image/jpeg")
        case url => fail(s"$url was not an url to the public bucket")
      }
    }
  }

  it should "reject nonsense sent as an image" in {
    val nonsense = Array.fill(MaxSizeInTest)((scala.util.Random.nextInt(256) - 128).toByte)
    post(uri = TeemakuvaUploadPath, body = nonsense, headers = Map("Content-Type" -> "image/jpeg")) {
      status should equal(415)
      body should include("image/jpeg")
      body should include("ei voitu lukea")
    }
  }

  it should "reject a jpeg image sent as a png" in {
    post(uri = TeemakuvaUploadPath, body = correctJpgTeemakuva, headers = Map("Content-Type" -> "image/png")) {
      status should equal(415)
      body should include("image/png")
      body should include("ei voitu lukea")
    }
  }

  it should "reject a png image sent as a jpeg" in {
    post(uri = TeemakuvaUploadPath, body = correctTeemakuva, headers = Map("Content-Type" -> "image/jpeg")) {
      status should equal(415)
      body should include("image/jpeg")
      body should include("ei voitu lukea")
    }
  }

  "Upload logo image" should "upload image" in {
    post(uri = LogoUploadPath, body = correctLogo, headers = Map("Content-Type" -> "image/png")) {
      status should equal(200)
      read[ImageUrl](body).url match {
        case s3Service.tempUrl(filename) =>
          val content = MockS3Client.getLocal(ImageBucket, s3Service.getTempKey(filename))
          content should not be empty
          val Content(localData, metadata) = content.get
          localData should equal(correctLogo)
          metadata.getCacheControl should equal("max-age=86400")
          metadata.getContentType should equal("image/png")
        case url => fail(s"$url was not an url to the public bucket")
      }
    }
  }

  it should "reject requests without a Content-Type header" in {
    post(uri = LogoUploadPath, body = correctLogo, headers = None) {
      withClue(body) {
        status should equal(400)
      }
      body should include("Content-Type")
    }
  }

  it should "reject requests with an unsupported Content-Type header" in {
    post(uri = LogoUploadPath, body = correctLogo, headers = Map("Content-Type" -> "image/bmp")) {
      withClue(body) {
        status should equal(415)
      }
      body should include("image/bmp")
    }
  }

  // At least the version of Apache HTTP client which Scalatra test stuff uses doesn't like it when a server returns
  // an error code and breaks the connection before the full request body has been sent. But it doesn't seem smart to
  // read the whole body, if we already know it will longer than the allowed size. So disabling this test.
  ignore should "reject too large images" in {
    val bigImage = new Array[Byte](100 * 1024 + 1)
    post(uri = LogoUploadPath, body = bigImage, headers = Map("Content-Type" -> "image/png")) {
      status should equal(413)
    }
  }

  it should "reject zero-size images" in {
    val zeroImage = new Array[Byte](0)
    post(uri = LogoUploadPath, body = zeroImage, headers = Map("Content-Type" -> "image/png")) {
      status should equal(400)
    }
  }

  it should "reject images that have too few pixels" in {
    post(uri = LogoUploadPath, body = tooSmallLogo, headers = Map("Content-Type" -> "image/png")) {
      status should equal(400)
      body should include("väärän kokoinen")
    }
  }

  it should "also accept jpeg images" in {
    post(uri = LogoUploadPath, body = correctJpgTeemakuva, headers = Map("Content-Type" -> "image/jpeg")) {
      status should equal(200)
      read[ImageUrl](body).url match {
        case s3Service.tempUrl(filename) =>
          val content = MockS3Client.getLocal(ImageBucket, s3Service.getTempKey(filename))
          content should not be empty
          val Content(localData, metadata) = content.get
          localData should equal(correctJpgTeemakuva)
          metadata.getCacheControl should equal("max-age=86400")
          metadata.getContentType should equal("image/jpeg")
        case url => fail(s"$url was not an url to the public bucket")
      }
    }
  }

  it should "reject nonsense sent as an image" in {
    val nonsense = Array.fill(20000)((scala.util.Random.nextInt(256) - 128).toByte)
    post(uri = LogoUploadPath, body = nonsense, headers = Map("Content-Type" -> "image/jpeg")) {
      status should equal(415)
      body should include("image/jpeg")
      body should include("ei voitu lukea")
    }
  }

  it should "reject a jpeg image sent as a png" in {
    post(uri = LogoUploadPath, body = correctJpgTeemakuva, headers = Map("Content-Type" -> "image/png")) {
      status should equal(415)
      body should include("image/png")
      body should include("ei voitu lukea")
    }
  }

  it should "reject a png image sent as a jpeg" in {
    post(uri = LogoUploadPath, body = correctLogo, headers = Map("Content-Type" -> "image/jpeg")) {
      status should equal(415)
      body should include("image/jpeg")
      body should include("ei voitu lukea")
    }
  }

}
