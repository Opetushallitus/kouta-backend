package fi.oph.kouta.indexing

import java.io.ByteArrayInputStream

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{CannedAccessControlList, ObjectMetadata, PutObjectRequest}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.utils.slf4j.Logging
import io.atlassian.aws.AmazonClientConnectionDef
import io.atlassian.aws.s3.S3Client

object S3ClientFactory {
  def create() = S3Client.create(
    config = Some(AmazonClientConnectionDef.default.copy(
      maxErrorRetry = Some(5),
      region = KoutaConfigurationFactory.configuration.s3Configuration.region.map(RegionUtils.getRegion))))
}

object S3Service extends S3Service(S3ClientFactory.create())

class S3Service(private val s3Client: AmazonS3Client) extends Logging {

  private val config = KoutaConfigurationFactory.configuration.s3Configuration

  logger.error(s"env profile: ${System.getenv("AWS_PROFILE")}")
  logger.error("Profile AWS ACCESS KEY ID: " + new ProfileCredentialsProvider().getCredentials.getAWSAccessKeyId)

  logger.error(s"AmazonClientConnectionDef.default.credential ${AmazonClientConnectionDef.default.credential}")

  logger.error("Default AWS ACCESS KEY ID: " + new DefaultAWSCredentialsProviderChain().getCredentials.getAWSAccessKeyId)

  def init(): Unit = ()

  def storeImage(key: String, contentType: String, content: Array[Byte]): String = {

    val metadata = new ObjectMetadata()
    metadata.setContentType(contentType)
    metadata.setContentLength(content.length)
    metadata.setCacheControl("max-age=86400") // 24 hours

    s3Client.putObject(
      new PutObjectRequest(config.imageBucket, key, new ByteArrayInputStream(content), metadata)
        .withCannedAcl(CannedAccessControlList.PublicRead))

    s"${config.imageBucketPublicUrl}/$key"
  }

}
