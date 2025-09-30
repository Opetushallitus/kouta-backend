package fi.oph.kouta.mocks

import fi.oph.kouta.config.S3Configuration
import fi.oph.kouta.images.{Image, ImageService}
import fi.oph.kouta.servlet.Authenticated

import java.util.UUID
import scala.collection.mutable
import scala.util.matching.Regex

class MockS3ImageService extends ImageService {
  val config: S3Configuration =
    S3Configuration(
      "konfo-files",
      "https://konfo-files.untuvaopintopolku.fi",
      "opintopolku-siirtotiedostot",
      "arn:aws:iam::xxxxxxxxxxxx:role/opintopolku-s3-cross-account-role",
      None,
      3,
      10000
    )
  val imageBucket: String = config.imageBucket
  val imageBucketPublicUrl: String = config.imageBucketPublicUrl
  val publicUrl: Regex = s"$imageBucketPublicUrl/(.*)".r
  val tempUrl: Regex = s"$imageBucketPublicUrl/temp/(.*)".r
  val storage: mutable.Map[String, Image] = mutable.Map.empty

  def getPublicUrl(key: String): String = s"$imageBucketPublicUrl/$key"

  def storeTempImage(image: Image)(implicit authenticated: Authenticated): String = {
    val url = s"$imageBucketPublicUrl/temp/${UUID.randomUUID}"
    storage.put(url, image)
    url
  }

  def copyImage(fromKey: String, toKey: String)(implicit authenticated: Authenticated): String = {
    val image = storage(s"$imageBucket/$fromKey")
    storage.put(s"$imageBucket/$toKey", image)
    storage.remove(s"$imageBucket/$fromKey")
    getPublicUrl(toKey)
  }
  
  def deleteImage(key: String)(implicit authenticated: Authenticated): Unit = {}
}

object MockS3ImageService extends MockS3ImageService
