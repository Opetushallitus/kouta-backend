package fi.oph.kouta.integration.fixture

import fi.oph.kouta.images.Image
import fi.oph.kouta.images.ImageFormat.Png
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.mocks.MockS3ImageService
import fi.oph.kouta.servlet.{ImageWithMinSizeSpecs, UploadServlet}
import org.scalatest.{Assertion, BeforeAndAfterEach}

import java.nio.file.{Files, Paths}

trait UploadFixture extends BeforeAndAfterEach {
  this: KoutaIntegrationSpec =>

  override def afterEach(): Unit = {
    super.afterEach()
    mockImageService.storage.clear
  }

  val UploadPath          = "/upload"
  val TeemakuvaUploadPath = s"$UploadPath/teemakuva"
  val LogoUploadPath      = s"$UploadPath/logo"
  val IconUploadPath      = s"$UploadPath/icon"

  val ImageBucket: String = MockS3ImageService.config.imageBucket
  val PublicImageServer: String = MockS3ImageService.config.imageBucketPublicUrl

  protected lazy val mockImageService: MockS3ImageService = MockS3ImageService

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

  def mockSaveImage(key: String): Unit = {
    mockImageService.storage.put(s"$ImageBucket/$key", Image(Png, correctTeemakuva))
  }

  def assertImageLocation(path: String, filename: String): Assertion = {
    val key = s"$ImageBucket/$path/$filename"
    val tempKey = s"$ImageBucket/temp/$filename"
    mockImageService.storage.get(key) should not be empty
    mockImageService.storage.get(tempKey) shouldBe empty
  }

  object TestUploadServlet extends UploadServlet(mockImageService) {
    override val teemakuvaSizes: ImageWithMinSizeSpecs = ImageWithMinSizeSpecs(maxSize = MaxSizeInTest, minWidth = 1260, minHeight = 400)
    override val logoSizes: ImageWithMinSizeSpecs = ImageWithMinSizeSpecs(maxSize = MaxSizeInTest, minWidth = 100, minHeight = 100)
  }
}
