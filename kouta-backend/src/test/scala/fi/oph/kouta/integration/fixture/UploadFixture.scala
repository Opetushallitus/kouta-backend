package fi.oph.kouta.integration.fixture

import java.nio.file.{Files, Paths}
import com.amazonaws.services.s3.model.ObjectMetadata
import fi.oph.kouta.images.{ImageServlet, S3ImageService}
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.integration.fixture.MockS3Client.Content
import fi.oph.kouta.mocks.MockS3ImageService
import fi.oph.kouta.servlet.{ImageSizeSpecs, ImageWithMinSizeSpecs, UploadServlet}
import org.scalatest.BeforeAndAfterEach

trait UploadFixture extends BeforeAndAfterEach {
  this: KoutaIntegrationSpec =>

  override def afterEach(): Unit = {
    super.afterEach()
    MockS3Client.reset()
  }

  val UploadPath          = "/upload"
  val TeemakuvaUploadPath = s"$UploadPath/teemakuva"
  val LogoUploadPath      = s"$UploadPath/logo"
  val IconUploadPath      = s"$UploadPath/icon"

  val ImageBucket       = MockS3ImageService.config.imageBucket
  val PublicImageServer = MockS3ImageService.config.imageBucketPublicUrl

  protected lazy val s3ImageService: S3ImageService = MockS3ImageService

  val MaxSizeInTest = 20000

  addServlet(TestUploadServlet, UploadPath)

  def getResourceImage(filename: String): Array[Byte] =
    Files.readAllBytes(Paths.get(getClass.getClassLoader.getResource(s"files/$filename").toURI))

  lazy val correctTeemakuva: Array[Byte]    = getResourceImage("correct-header.png")
  lazy val smallTeemakuva: Array[Byte]      = getResourceImage("small-teemakuva.png")
  lazy val tooSmallSmallTeemakuva: Array[Byte] = getResourceImage("too-small-small-teemakuva.png")
  lazy val correctLogo: Array[Byte]         = getResourceImage("correct-logo.png")
  lazy val correctJpgTeemakuva: Array[Byte] = getResourceImage("correct-header.jpg")
  lazy val tooLargeTeemakuva: Array[Byte]   = getResourceImage("too-large-header.png")
  lazy val tooSmallHeader: Array[Byte]      = getResourceImage("too-small-header.png")
  lazy val tooSmallLogo: Array[Byte]        = getResourceImage("too-small-logo.png")
  lazy val correctSvgLogo: Array[Byte]      = getResourceImage("correct-logo.svg")
  lazy val randomXml: Array[Byte]           = getResourceImage("random-xml.svg")
  lazy val correctIcon: Array[Byte]         = getResourceImage("correct-icon.png")

  def saveLocalPng(key: String): Unit = {
    val metadata = new ObjectMetadata()
    metadata.setCacheControl("max-age=1337")
    metadata.setContentType("image/png")
    MockS3Client.putLocal(ImageBucket, key, correctTeemakuva, metadata)
  }

  def checkLocalPng(content: Option[Content]) = {
    content should not be empty
    val Content(imageData, returnedMeta) = content.get
    imageData should equal(correctTeemakuva)
    returnedMeta.getCacheControl should equal("max-age=1337")
    returnedMeta.getContentLength should equal(correctTeemakuva.length)
    returnedMeta.getContentType should equal("image/png")
  }

  object TestUploadServlet extends UploadServlet(s3ImageService) {
    override val teemakuvaSizes: ImageWithMinSizeSpecs = ImageWithMinSizeSpecs(maxSize = MaxSizeInTest, minWidth = 1260, minHeight = 400)
    override val logoSizes: ImageWithMinSizeSpecs = ImageWithMinSizeSpecs(maxSize = MaxSizeInTest, minWidth = 100, minHeight = 100)
  }
}
