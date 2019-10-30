package fi.oph.kouta.indexing

import java.io.ByteArrayInputStream
import java.util.UUID

import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{CannedAccessControlList, CopyObjectRequest, ObjectMetadata, PutObjectRequest}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.utils.slf4j.Logging
import io.atlassian.aws.AmazonClientConnectionDef
import io.atlassian.aws.s3.S3Client

import scala.util.matching.Regex

object S3ClientFactory {
  def create(): AmazonS3Client = S3Client.create(
    config = Some(AmazonClientConnectionDef.default.copy(
      maxErrorRetry = Some(5),
      region = KoutaConfigurationFactory.configuration.s3Configuration.region.map(RegionUtils.getRegion))))
}

object S3Service extends S3Service(S3ClientFactory.create())

class S3Service(private val s3Client: AmazonS3Client) extends Logging {

  private val config = KoutaConfigurationFactory.configuration.s3Configuration

  def getPublicUrl(key: String) = s"${config.imageBucketPublicUrl}/$key"

  def getTempKey(filename: String) = s"temp/$filename"

  val publicUrl: Regex = s"(${config.imageBucketPublicUrl}/)(.*)".r
  val tempUrl: Regex = s"(${config.imageBucketPublicUrl}/temp/)(.*)".r

  def storeTempImage(extension: String, contentType: String, content: Array[Byte]): String = {
    val key = getTempKey(s"${UUID.randomUUID}.$extension")
    storeImage(key, contentType, content)
  }

  def storeImage(key: String, contentType: String, content: Array[Byte]): String = {
    val metadata = new ObjectMetadata()
    metadata.setContentType(contentType)
    metadata.setContentLength(content.length)
    metadata.setCacheControl("max-age=86400") // 24 hours

    s3Client.putObject(
      new PutObjectRequest(config.imageBucket, key, new ByteArrayInputStream(content), metadata)
        .withCannedAcl(CannedAccessControlList.PublicRead))

    getPublicUrl(key)
  }

  def copyImage(fromKey: String, toKey: String): String = {
    val request = new CopyObjectRequest(config.imageBucket, fromKey, config.imageBucket, toKey)
      .withCannedAccessControlList(CannedAccessControlList.PublicRead)

    logger.info(s"Copying s3://${config.imageBucket}/$fromKey to s3://${config.imageBucket}/$toKey")
    s3Client.copyObject(request)

    getPublicUrl(toKey)
  }

  def deleteImage(key: String): Unit = {
    logger.info(s"Deleting s3://${config.imageBucket}/$key")
    s3Client.deleteObject(config.imageBucket, key)
  }
}
