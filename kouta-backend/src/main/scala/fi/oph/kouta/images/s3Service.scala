package fi.oph.kouta.images

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.config.{KoutaConfigurationFactory, S3Configuration}
import fi.oph.kouta.logging.Logging
import fi.oph.kouta.servlet.Authenticated
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{
  CopyObjectRequest,
  DeleteObjectRequest,
  ObjectCannedACL,
  PutObjectRequest
}

import java.io.ByteArrayInputStream
import java.util.UUID
import scala.util.matching.Regex

object S3ClientFactory {
  def create(): S3Client =
    S3Client
      .builder()
      .region(KoutaConfigurationFactory.configuration.s3Configuration.region.map(Region.of).getOrElse(Region.EU_WEST_1))
      .build()
}

trait ImageService {
  val publicUrl: Regex
  val tempUrl: Regex
  def getPublicUrl(key: String): String
  def getTempKey(filename: String): String = s"temp/$filename"
  def storeTempImage(image: Image)(implicit authenticated: Authenticated): String
  def copyImage(fromKey: String, toKey: String)(implicit authenticated: Authenticated): String
  def deleteImage(key: String)(implicit authenticated: Authenticated): Unit
}

object S3ImageService extends S3ImageService(S3ClientFactory.create(), AuditLog)

class S3ImageService(private val s3Client: S3Client, auditLog: AuditLog) extends ImageService with Logging {

  lazy val config: S3Configuration = KoutaConfigurationFactory.configuration.s3Configuration

  def getPublicUrl(key: String): String = s"${config.imageBucketPublicUrl}/$key"

  lazy val publicUrl: Regex = s"${config.imageBucketPublicUrl}/(.*)".r
  lazy val tempUrl: Regex = s"${config.imageBucketPublicUrl}/temp/(.*)".r

  def storeTempImage(image: Image)(implicit authenticated: Authenticated): String = {
    val extension = image.format.extension
    val key = getTempKey(s"${UUID.randomUUID}.$extension")
    storeImage(key, image)
  }

  private def storeImage(key: String, image: Image)(implicit authenticated: Authenticated): String = {
    val putObjectRequest = PutObjectRequest
      .builder()
      .bucket(config.imageBucket)
      .key(key)
      .acl(ObjectCannedACL.PUBLIC_READ)
      .contentType(image.format.contentType)
      .contentLength(image.data.length)
      .cacheControl("max-age=86400") // 24 hours
      .build()

    val inputStream = new ByteArrayInputStream(image.data)
    val requestBody = RequestBody.fromInputStream(inputStream, inputStream.available())
    s3Client.putObject(putObjectRequest, requestBody)

    auditLog.logS3Upload(s"s3://${config.imageBucket}/$key")
    getPublicUrl(key)
  }

  def copyImage(fromKey: String, toKey: String)(implicit authenticated: Authenticated): String = {
    val copyObjectRequest = CopyObjectRequest
      .builder()
      .sourceBucket(config.imageBucket)
      .sourceKey(fromKey)
      .destinationBucket(config.imageBucket)
      .destinationKey(toKey)
      .acl(ObjectCannedACL.PUBLIC_READ)
      .build()
    s3Client.copyObject(copyObjectRequest)

    auditLog.logS3Copy(s"s3://${config.imageBucket}/$fromKey", s"s3://${config.imageBucket}/$toKey")
    getPublicUrl(toKey)
  }

  def deleteImage(key: String)(implicit authenticated: Authenticated): Unit = {
    val deleteObjectRequest = DeleteObjectRequest
      .builder()
      .bucket(config.imageBucket)
      .key(key)
      .build()
    s3Client.deleteObject(deleteObjectRequest)

    auditLog.logS3Delete(s"s3://${config.imageBucket}/$key")
  }
}
