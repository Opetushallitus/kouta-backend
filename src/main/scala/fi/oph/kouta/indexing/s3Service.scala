package fi.oph.kouta.indexing

import java.io.ByteArrayInputStream
import java.util.UUID

import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.s3.model.{CannedAccessControlList, CopyObjectRequest, ObjectMetadata, PutObjectRequest}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client}
import fi.oph.kouta.config.{KoutaConfigurationFactory, S3Configuration}
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

object S3Service extends S3Service(S3ClientFactory.create())

abstract class ImageType(val contentType: String, val extension: String) extends Product with Serializable

object ImageType {
  case object Jpeg extends ImageType("image/jpeg", "jpg")
  case object Png extends ImageType("image/png", "png")

  val allowedTypes = Set(Jpeg, Png)
  val typeMap: Map[String, ImageType] = allowedTypes.map(t => t.contentType -> t).toMap

  def get(contentType: String): Option[ImageType] = typeMap.get(contentType)
}

class S3Service(private val s3Client: AmazonS3) extends Logging {

  lazy val config: S3Configuration = KoutaConfigurationFactory.configuration.s3Configuration

  def getPublicUrl(key: String) = s"${config.imageBucketPublicUrl}/$key"

  def getTempKey(filename: String) = s"temp/$filename"

  lazy val publicUrl: Regex = s"${config.imageBucketPublicUrl}/(.*)".r
  lazy val tempUrl: Regex = s"${config.imageBucketPublicUrl}/temp/(.*)".r

  def storeTempImage(imageType: ImageType, imageData: Array[Byte]): String = {
    val extension = imageType.extension
    val key = getTempKey(s"${UUID.randomUUID}.$extension")
    storeImage(key, imageType, imageData)
  }

  def storeImage(key: String, imageType: ImageType, imageData: Array[Byte]): String = {
    val metadata = new ObjectMetadata()
    metadata.setContentType(imageType.contentType)
    metadata.setContentLength(imageData.length)
    metadata.setCacheControl("max-age=86400") // 24 hours

    logger.info(s"Creating s3://${config.imageBucket}/$key")
    s3Client.putObject(
      new PutObjectRequest(config.imageBucket, key, new ByteArrayInputStream(imageData), metadata)
        .withCannedAcl(CannedAccessControlList.PublicRead)
    )

    getPublicUrl(key)
  }

  def copyImage(fromKey: String, toKey: String): String = {
    logger.info(s"Copying s3://${config.imageBucket}/$fromKey to s3://${config.imageBucket}/$toKey")
    s3Client.copyObject(
      new CopyObjectRequest(config.imageBucket, fromKey, config.imageBucket, toKey)
        .withCannedAccessControlList(CannedAccessControlList.PublicRead)
    )

    getPublicUrl(toKey)
  }

  def deleteImage(key: String): Unit = {
    logger.info(s"Deleting s3://${config.imageBucket}/$key")
    s3Client.deleteObject(config.imageBucket, key)
  }
}
