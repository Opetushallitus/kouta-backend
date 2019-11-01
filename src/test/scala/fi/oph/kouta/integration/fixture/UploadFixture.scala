package fi.oph.kouta.integration.fixture

import java.nio.file.{Files, Paths}

import com.amazonaws.services.s3.model.ObjectMetadata
import fi.oph.kouta.config.S3Configuration
import fi.oph.kouta.indexing.S3Service
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.integration.fixture.MockS3Client.Content
import fi.oph.kouta.servlet.UploadServlet

object MockS3Service extends S3Service(MockS3Client) {
  override lazy val config = S3Configuration("konfo-files", "https://testibucket.fi", None)
}

trait UploadFixture {
  this: KoutaIntegrationSpec =>

  val UploadPath = "/upload"
  val ThemeImageUploadPath = s"$UploadPath/theme-image"

  val ImageBucket = MockS3Service.config.imageBucket
  val PublicImagePath = MockS3Service.config.imageBucketPublicUrl

  protected lazy val s3Service: S3Service = MockS3Service

  addServlet(new UploadServlet(s3Service), UploadPath)

  def getResourceImage(filename: String) = Files.readAllBytes(Paths.get(getClass.getClassLoader.getResource(s"files/$filename").toURI))

  lazy val correctTheme: Array[Byte] = getResourceImage("correct-header.png")
  lazy val correctJpgTheme: Array[Byte] = getResourceImage("correct-header.jpg")
  lazy val tooLargeTheme: Array[Byte] = getResourceImage("too-large-header.png")
  lazy val tooSmallHeader: Array[Byte] = getResourceImage("too-small-header.png")

  def saveLocalPng(key: String): Unit = {
    val metadata = new ObjectMetadata()
    metadata.setCacheControl("max-age=1337")
    metadata.setContentType("image/png")
    MockS3Client.putLocal(ImageBucket, key, correctTheme, metadata)
  }

  def checkLocalPng(content: Option[Content]) = {
    content should not be empty
    val Content(imageData, returnedMeta) = content.get
    imageData should equal(correctTheme)
    returnedMeta.getCacheControl should equal("max-age=1337")
    returnedMeta.getContentLength should equal(correctTheme.length)
    returnedMeta.getContentType should equal("image/png")
  }

}
