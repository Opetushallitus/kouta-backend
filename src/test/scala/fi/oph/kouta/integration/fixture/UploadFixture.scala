package fi.oph.kouta.integration.fixture

import java.nio.file.{Files, Paths}

import fi.oph.kouta.indexing.S3Service
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.servlet.UploadServlet

object FakeS3Service extends S3Service(FakeS3Client)

trait UploadFixture {
  this: KoutaIntegrationSpec =>

  val UploadPath = "/upload"
  val ThemeImageUploadPath = s"$UploadPath/theme-image"

  val Bucket = "konfo-files.untuvaopintopolku.fi"
  val PublicPath = "https://konfo-files.untuvaopintopolku.fi"

  protected lazy val s3Service: S3Service = FakeS3Service

  addServlet(new UploadServlet(s3Service), UploadPath)

  def getResourceImage(filename: String) = Files.readAllBytes(Paths.get(getClass.getClassLoader.getResource(s"files/$filename").toURI))

  lazy val correctTheme: Array[Byte] = getResourceImage("correct-header.png")
  lazy val correctJpgTheme: Array[Byte] = getResourceImage("correct-header.jpg")
  lazy val tooLargeTheme: Array[Byte] = getResourceImage("too-large-header.png")
  lazy val tooSmallHeader: Array[Byte] = getResourceImage("too-small-header.png")

}
