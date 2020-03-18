package fi.oph.kouta.images

import java.io.ByteArrayInputStream
import java.util.UUID

import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.s3.model.{CannedAccessControlList, CopyObjectRequest, ObjectMetadata, PutObjectRequest}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client}
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.config.{KoutaConfigurationFactory, S3Configuration}
import fi.oph.kouta.servlet.Authenticated
import fi.vm.sade.utils.slf4j.Logging
import io.atlassian.aws.AmazonClientConnectionDef
import io.atlassian.aws.s3.S3Client

import scala.util.matching.Regex

object S3ClientFactory {
  def create(): AmazonS3Client =
    S3Client.create(
      config = Some(
        AmazonClientConnectionDef.default.copy(
          maxErrorRetry = Some(5),
          region = KoutaConfigurationFactory.configuration.s3Configuration.region.map(RegionUtils.getRegion)
        )
      )
    )
}

object S3ImageService extends S3ImageService(S3ClientFactory.create(), AuditLog)

class S3ImageService(private val s3Client: AmazonS3, auditLog: AuditLog) extends Logging {

  lazy val config: S3Configuration = KoutaConfigurationFactory.configuration.s3Configuration

  def getPublicUrl(key: String) = s"${config.imageBucketPublicUrl}/$key"

  def getTempKey(filename: String) = s"temp/$filename"

  lazy val publicUrl: Regex = s"${config.imageBucketPublicUrl}/(.*)".r
  lazy val tempUrl: Regex = s"${config.imageBucketPublicUrl}/temp/(.*)".r

  def storeTempImage(image: Image)(implicit authenticated: Authenticated): String = {
    val extension = image.format.extension
    val key = getTempKey(s"${UUID.randomUUID}.$extension")
    storeImage(key, image)
  }

  def storeImage(key: String, image: Image)(implicit authenticated: Authenticated): String = {
    val metadata = new ObjectMetadata()
    metadata.setContentType(image.format.contentType)
    metadata.setContentLength(image.data.length)
    metadata.setCacheControl("max-age=86400") // 24 hours

    s3Client.putObject(
      new PutObjectRequest(config.imageBucket, key, new ByteArrayInputStream(image.data), metadata)
        .withCannedAcl(CannedAccessControlList.PublicRead)
    )

    auditLog.logS3Upload(s"s3://${config.imageBucket}/$key")
    getPublicUrl(key)
  }

  def copyImage(fromKey: String, toKey: String)(implicit authenticated: Authenticated): String = {
    s3Client.copyObject(
      new CopyObjectRequest(config.imageBucket, fromKey, config.imageBucket, toKey)
        .withCannedAccessControlList(CannedAccessControlList.PublicRead)
    )

    auditLog.logS3Copy(s"s3://${config.imageBucket}/$fromKey", s"s3://${config.imageBucket}/$toKey")
    getPublicUrl(toKey)
  }

  def deleteImage(key: String)(implicit authenticated: Authenticated): Unit = {
    s3Client.deleteObject(config.imageBucket, key)
    auditLog.logS3Delete(s"s3://${config.imageBucket}/$key")
  }
}
