package fi.oph.kouta.integration.fixture

import java.io.{DataInputStream, File, InputStream}
import java.net.URL
import java.util
import java.util.Date

import com.amazonaws.regions.Region
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.model.analytics.AnalyticsConfiguration
import com.amazonaws.services.s3.model.inventory.InventoryConfiguration
import com.amazonaws.services.s3.model.metrics.MetricsConfiguration
import com.amazonaws.services.s3.waiters.AmazonS3Waiters
import com.amazonaws.services.s3.{AmazonS3, S3ClientOptions, S3ResponseMetadata, model}
import com.amazonaws.{AmazonWebServiceRequest, HttpMethod}

import scala.collection.mutable

object MockS3Client extends AmazonS3 {

  case class Content(data: Array[Byte], metadata: ObjectMetadata) {

    def contentType: String = metadata.getContentType

    def cacheControl: String = metadata.getCacheControl
  }

  val storage: mutable.Map[String, Content] = mutable.Map.empty

  def getLocal(bucket: String, key: String): Option[Content] = storage.get(s"$bucket/$key")

  def reset(): Unit = storage.clear()

  override def putObject(putObjectRequest: PutObjectRequest): PutObjectResult = {
    val key = s"${putObjectRequest.getBucketName}/${putObjectRequest.getKey}"
    val metadata = putObjectRequest.getMetadata

    val length = metadata.getContentLength.toInt
    val data = new Array[Byte](length)
    val stream = new DataInputStream(putObjectRequest.getInputStream)
    stream.readFully(data, 0, length)
    stream.close()

    storage.put(key, Content(data, metadata))

    null // Paluuarvoa ei käytetä ATM, joten se voi ihan yhtä hyvin olla null
  }

  def putLocal(bucket: String, key: String, data: Array[Byte], metadata: ObjectMetadata): Unit = {
    metadata.setContentLength(data.length)
    storage.put(s"$bucket/$key", Content(data, metadata))
  }

  override def copyObject(copyObjectRequest: CopyObjectRequest): CopyObjectResult = {
    val fromKey = s"${copyObjectRequest.getSourceBucketName}/${copyObjectRequest.getSourceKey}"
    val toKey = s"${copyObjectRequest.getDestinationBucketName}/${copyObjectRequest.getDestinationKey}"

    storage.put(toKey, storage(fromKey))

    null // Paluuarvoa ei käytetä ATM, joten se voi ihan yhtä hyvin olla null
  }

  override def deleteObject(bucketName: String, key: String): Unit = {
    storage.remove(s"$bucketName/$key")
  }

  // Iso kasa metodeja ilman toteutuksia. Vähemmänllä rivimäärällä selviäisi, jos perisi toteutuksen, eikä toteuttaisi koko rajapintaa.
  // Mutta näin ei ainakaan tule vahingossa kutsuttua mitään metodia, josta pyyntö lähteekin AWS:lle.


  override def setEndpoint(s: String): Unit = ???

  override def setRegion(region: Region): Unit = ???

  override def setS3ClientOptions(s3ClientOptions: S3ClientOptions): Unit = ???

  override def changeObjectStorageClass(s: String, s1: String, storageClass: StorageClass): Unit = ???

  override def setObjectRedirectLocation(s: String, s1: String, s2: String): Unit = ???

  override def listObjects(s: String): ObjectListing = ???

  override def listObjects(s: String, s1: String): ObjectListing = ???

  override def listObjects(listObjectsRequest: ListObjectsRequest): ObjectListing = ???

  override def listObjectsV2(s: String): ListObjectsV2Result = ???

  override def listObjectsV2(s: String, s1: String): ListObjectsV2Result = ???

  override def listObjectsV2(listObjectsV2Request: ListObjectsV2Request): ListObjectsV2Result = ???

  override def listNextBatchOfObjects(objectListing: ObjectListing): ObjectListing = ???

  override def listNextBatchOfObjects(listNextBatchOfObjectsRequest: ListNextBatchOfObjectsRequest): ObjectListing = ???

  override def listVersions(s: String, s1: String): VersionListing = ???

  override def listNextBatchOfVersions(versionListing: VersionListing): VersionListing = ???

  override def listNextBatchOfVersions(listNextBatchOfVersionsRequest: ListNextBatchOfVersionsRequest): VersionListing = ???

  override def listVersions(s: String, s1: String, s2: String, s3: String, s4: String, integer: Integer): VersionListing = ???

  override def listVersions(listVersionsRequest: ListVersionsRequest): VersionListing = ???

  override def getS3AccountOwner: Owner = ???

  override def getS3AccountOwner(getS3AccountOwnerRequest: GetS3AccountOwnerRequest): Owner = ???

  override def doesBucketExist(s: String): Boolean = ???

  override def headBucket(headBucketRequest: HeadBucketRequest): HeadBucketResult = ???

  override def listBuckets(): util.List[Bucket] = ???

  override def listBuckets(listBucketsRequest: ListBucketsRequest): util.List[Bucket] = ???

  override def getBucketLocation(s: String): String = ???

  override def getBucketLocation(getBucketLocationRequest: GetBucketLocationRequest): String = ???

  override def createBucket(createBucketRequest: CreateBucketRequest): Bucket = ???

  override def createBucket(s: String): Bucket = ???

  override def createBucket(s: String, region: model.Region): Bucket = ???

  override def createBucket(s: String, s1: String): Bucket = ???

  override def getObjectAcl(s: String, s1: String): AccessControlList = ???

  override def getObjectAcl(s: String, s1: String, s2: String): AccessControlList = ???

  override def getObjectAcl(getObjectAclRequest: GetObjectAclRequest): AccessControlList = ???

  override def setObjectAcl(s: String, s1: String, accessControlList: AccessControlList): Unit = ???

  override def setObjectAcl(s: String, s1: String, cannedAccessControlList: CannedAccessControlList): Unit = ???

  override def setObjectAcl(s: String, s1: String, s2: String, accessControlList: AccessControlList): Unit = ???

  override def setObjectAcl(s: String, s1: String, s2: String, cannedAccessControlList: CannedAccessControlList): Unit = ???

  override def setObjectAcl(setObjectAclRequest: SetObjectAclRequest): Unit = ???

  override def getBucketAcl(s: String): AccessControlList = ???

  override def setBucketAcl(setBucketAclRequest: SetBucketAclRequest): Unit = ???

  override def getBucketAcl(getBucketAclRequest: GetBucketAclRequest): AccessControlList = ???

  override def setBucketAcl(s: String, accessControlList: AccessControlList): Unit = ???

  override def setBucketAcl(s: String, cannedAccessControlList: CannedAccessControlList): Unit = ???

  override def getObjectMetadata(s: String, s1: String): ObjectMetadata = ???

  override def getObjectMetadata(getObjectMetadataRequest: GetObjectMetadataRequest): ObjectMetadata = ???

  override def getObject(s: String, s1: String): S3Object = ???

  override def getObject(getObjectRequest: GetObjectRequest): S3Object = ???

  override def getObject(getObjectRequest: GetObjectRequest, file: File): ObjectMetadata = ???

  override def getObjectAsString(s: String, s1: String): String = ???

  override def getObjectTagging(getObjectTaggingRequest: GetObjectTaggingRequest): GetObjectTaggingResult = ???

  override def setObjectTagging(setObjectTaggingRequest: SetObjectTaggingRequest): SetObjectTaggingResult = ???

  override def deleteObjectTagging(deleteObjectTaggingRequest: DeleteObjectTaggingRequest): DeleteObjectTaggingResult = ???

  override def deleteBucket(deleteBucketRequest: DeleteBucketRequest): Unit = ???

  override def deleteBucket(s: String): Unit = ???

  override def putObject(s: String, s1: String, file: File): PutObjectResult = ???

  override def putObject(s: String, s1: String, inputStream: InputStream, objectMetadata: ObjectMetadata): PutObjectResult = ???

  override def putObject(s: String, s1: String, s2: String): PutObjectResult = ???

  override def copyObject(s: String, s1: String, s2: String, s3: String): CopyObjectResult = ???

  override def copyPart(copyPartRequest: CopyPartRequest): CopyPartResult = ???

  override def deleteObject(deleteObjectRequest: DeleteObjectRequest): Unit = ???

  override def deleteObjects(deleteObjectsRequest: DeleteObjectsRequest): DeleteObjectsResult = ???

  override def deleteVersion(s: String, s1: String, s2: String): Unit = ???

  override def deleteVersion(deleteVersionRequest: DeleteVersionRequest): Unit = ???

  override def getBucketLoggingConfiguration(s: String): BucketLoggingConfiguration = ???

  override def getBucketLoggingConfiguration(getBucketLoggingConfigurationRequest: GetBucketLoggingConfigurationRequest): BucketLoggingConfiguration = ???

  override def setBucketLoggingConfiguration(setBucketLoggingConfigurationRequest: SetBucketLoggingConfigurationRequest): Unit = ???

  override def getBucketVersioningConfiguration(s: String): BucketVersioningConfiguration = ???

  override def getBucketVersioningConfiguration(getBucketVersioningConfigurationRequest: GetBucketVersioningConfigurationRequest): BucketVersioningConfiguration = ???

  override def setBucketVersioningConfiguration(setBucketVersioningConfigurationRequest: SetBucketVersioningConfigurationRequest): Unit = ???

  override def getBucketLifecycleConfiguration(s: String): BucketLifecycleConfiguration = ???

  override def getBucketLifecycleConfiguration(getBucketLifecycleConfigurationRequest: GetBucketLifecycleConfigurationRequest): BucketLifecycleConfiguration = ???

  override def setBucketLifecycleConfiguration(s: String, bucketLifecycleConfiguration: BucketLifecycleConfiguration): Unit = ???

  override def setBucketLifecycleConfiguration(setBucketLifecycleConfigurationRequest: SetBucketLifecycleConfigurationRequest): Unit = ???

  override def deleteBucketLifecycleConfiguration(s: String): Unit = ???

  override def deleteBucketLifecycleConfiguration(deleteBucketLifecycleConfigurationRequest: DeleteBucketLifecycleConfigurationRequest): Unit = ???

  override def getBucketCrossOriginConfiguration(s: String): BucketCrossOriginConfiguration = ???

  override def getBucketCrossOriginConfiguration(getBucketCrossOriginConfigurationRequest: GetBucketCrossOriginConfigurationRequest): BucketCrossOriginConfiguration = ???

  override def setBucketCrossOriginConfiguration(s: String, bucketCrossOriginConfiguration: BucketCrossOriginConfiguration): Unit = ???

  override def setBucketCrossOriginConfiguration(setBucketCrossOriginConfigurationRequest: SetBucketCrossOriginConfigurationRequest): Unit = ???

  override def deleteBucketCrossOriginConfiguration(s: String): Unit = ???

  override def deleteBucketCrossOriginConfiguration(deleteBucketCrossOriginConfigurationRequest: DeleteBucketCrossOriginConfigurationRequest): Unit = ???

  override def getBucketTaggingConfiguration(s: String): BucketTaggingConfiguration = ???

  override def getBucketTaggingConfiguration(getBucketTaggingConfigurationRequest: GetBucketTaggingConfigurationRequest): BucketTaggingConfiguration = ???

  override def setBucketTaggingConfiguration(s: String, bucketTaggingConfiguration: BucketTaggingConfiguration): Unit = ???

  override def setBucketTaggingConfiguration(setBucketTaggingConfigurationRequest: SetBucketTaggingConfigurationRequest): Unit = ???

  override def deleteBucketTaggingConfiguration(s: String): Unit = ???

  override def deleteBucketTaggingConfiguration(deleteBucketTaggingConfigurationRequest: DeleteBucketTaggingConfigurationRequest): Unit = ???

  override def getBucketNotificationConfiguration(s: String): BucketNotificationConfiguration = ???

  override def getBucketNotificationConfiguration(getBucketNotificationConfigurationRequest: GetBucketNotificationConfigurationRequest): BucketNotificationConfiguration = ???

  override def setBucketNotificationConfiguration(setBucketNotificationConfigurationRequest: SetBucketNotificationConfigurationRequest): Unit = ???

  override def setBucketNotificationConfiguration(s: String, bucketNotificationConfiguration: BucketNotificationConfiguration): Unit = ???

  override def getBucketWebsiteConfiguration(s: String): BucketWebsiteConfiguration = ???

  override def getBucketWebsiteConfiguration(getBucketWebsiteConfigurationRequest: GetBucketWebsiteConfigurationRequest): BucketWebsiteConfiguration = ???

  override def setBucketWebsiteConfiguration(s: String, bucketWebsiteConfiguration: BucketWebsiteConfiguration): Unit = ???

  override def setBucketWebsiteConfiguration(setBucketWebsiteConfigurationRequest: SetBucketWebsiteConfigurationRequest): Unit = ???

  override def deleteBucketWebsiteConfiguration(s: String): Unit = ???

  override def deleteBucketWebsiteConfiguration(deleteBucketWebsiteConfigurationRequest: DeleteBucketWebsiteConfigurationRequest): Unit = ???

  override def getBucketPolicy(s: String): BucketPolicy = ???

  override def getBucketPolicy(getBucketPolicyRequest: GetBucketPolicyRequest): BucketPolicy = ???

  override def setBucketPolicy(s: String, s1: String): Unit = ???

  override def setBucketPolicy(setBucketPolicyRequest: SetBucketPolicyRequest): Unit = ???

  override def deleteBucketPolicy(s: String): Unit = ???

  override def deleteBucketPolicy(deleteBucketPolicyRequest: DeleteBucketPolicyRequest): Unit = ???

  override def generatePresignedUrl(s: String, s1: String, date: Date): URL = ???

  override def generatePresignedUrl(s: String, s1: String, date: Date, httpMethod: HttpMethod): URL = ???

  override def generatePresignedUrl(generatePresignedUrlRequest: GeneratePresignedUrlRequest): URL = ???

  override def initiateMultipartUpload(initiateMultipartUploadRequest: InitiateMultipartUploadRequest): InitiateMultipartUploadResult = ???

  override def uploadPart(uploadPartRequest: UploadPartRequest): UploadPartResult = ???

  override def listParts(listPartsRequest: ListPartsRequest): PartListing = ???

  override def abortMultipartUpload(abortMultipartUploadRequest: AbortMultipartUploadRequest): Unit = ???

  override def completeMultipartUpload(completeMultipartUploadRequest: CompleteMultipartUploadRequest): CompleteMultipartUploadResult = ???

  override def listMultipartUploads(listMultipartUploadsRequest: ListMultipartUploadsRequest): MultipartUploadListing = ???

  override def getCachedResponseMetadata(amazonWebServiceRequest: AmazonWebServiceRequest): S3ResponseMetadata = ???

  override def restoreObject(restoreObjectRequest: RestoreObjectRequest): Unit = ???

  override def restoreObject(s: String, s1: String, i: Int): Unit = ???

  override def enableRequesterPays(s: String): Unit = ???

  override def disableRequesterPays(s: String): Unit = ???

  override def isRequesterPaysEnabled(s: String): Boolean = ???

  override def setBucketReplicationConfiguration(s: String, bucketReplicationConfiguration: BucketReplicationConfiguration): Unit = ???

  override def setBucketReplicationConfiguration(setBucketReplicationConfigurationRequest: SetBucketReplicationConfigurationRequest): Unit = ???

  override def getBucketReplicationConfiguration(s: String): BucketReplicationConfiguration = ???

  override def getBucketReplicationConfiguration(getBucketReplicationConfigurationRequest: GetBucketReplicationConfigurationRequest): BucketReplicationConfiguration = ???

  override def deleteBucketReplicationConfiguration(s: String): Unit = ???

  override def deleteBucketReplicationConfiguration(deleteBucketReplicationConfigurationRequest: DeleteBucketReplicationConfigurationRequest): Unit = ???

  override def doesObjectExist(s: String, s1: String): Boolean = ???

  override def getBucketAccelerateConfiguration(s: String): BucketAccelerateConfiguration = ???

  override def getBucketAccelerateConfiguration(getBucketAccelerateConfigurationRequest: GetBucketAccelerateConfigurationRequest): BucketAccelerateConfiguration = ???

  override def setBucketAccelerateConfiguration(s: String, bucketAccelerateConfiguration: BucketAccelerateConfiguration): Unit = ???

  override def setBucketAccelerateConfiguration(setBucketAccelerateConfigurationRequest: SetBucketAccelerateConfigurationRequest): Unit = ???

  override def deleteBucketMetricsConfiguration(s: String, s1: String): DeleteBucketMetricsConfigurationResult = ???

  override def deleteBucketMetricsConfiguration(deleteBucketMetricsConfigurationRequest: DeleteBucketMetricsConfigurationRequest): DeleteBucketMetricsConfigurationResult = ???

  override def getBucketMetricsConfiguration(s: String, s1: String): GetBucketMetricsConfigurationResult = ???

  override def getBucketMetricsConfiguration(getBucketMetricsConfigurationRequest: GetBucketMetricsConfigurationRequest): GetBucketMetricsConfigurationResult = ???

  override def setBucketMetricsConfiguration(s: String, metricsConfiguration: MetricsConfiguration): SetBucketMetricsConfigurationResult = ???

  override def setBucketMetricsConfiguration(setBucketMetricsConfigurationRequest: SetBucketMetricsConfigurationRequest): SetBucketMetricsConfigurationResult = ???

  override def listBucketMetricsConfigurations(listBucketMetricsConfigurationsRequest: ListBucketMetricsConfigurationsRequest): ListBucketMetricsConfigurationsResult = ???

  override def deleteBucketAnalyticsConfiguration(s: String, s1: String): DeleteBucketAnalyticsConfigurationResult = ???

  override def deleteBucketAnalyticsConfiguration(deleteBucketAnalyticsConfigurationRequest: DeleteBucketAnalyticsConfigurationRequest): DeleteBucketAnalyticsConfigurationResult = ???

  override def getBucketAnalyticsConfiguration(s: String, s1: String): GetBucketAnalyticsConfigurationResult = ???

  override def getBucketAnalyticsConfiguration(getBucketAnalyticsConfigurationRequest: GetBucketAnalyticsConfigurationRequest): GetBucketAnalyticsConfigurationResult = ???

  override def setBucketAnalyticsConfiguration(s: String, analyticsConfiguration: AnalyticsConfiguration): SetBucketAnalyticsConfigurationResult = ???

  override def setBucketAnalyticsConfiguration(setBucketAnalyticsConfigurationRequest: SetBucketAnalyticsConfigurationRequest): SetBucketAnalyticsConfigurationResult = ???

  override def listBucketAnalyticsConfigurations(listBucketAnalyticsConfigurationsRequest: ListBucketAnalyticsConfigurationsRequest): ListBucketAnalyticsConfigurationsResult = ???

  override def deleteBucketInventoryConfiguration(s: String, s1: String): DeleteBucketInventoryConfigurationResult = ???

  override def deleteBucketInventoryConfiguration(deleteBucketInventoryConfigurationRequest: DeleteBucketInventoryConfigurationRequest): DeleteBucketInventoryConfigurationResult = ???

  override def getBucketInventoryConfiguration(s: String, s1: String): GetBucketInventoryConfigurationResult = ???

  override def getBucketInventoryConfiguration(getBucketInventoryConfigurationRequest: GetBucketInventoryConfigurationRequest): GetBucketInventoryConfigurationResult = ???

  override def setBucketInventoryConfiguration(s: String, inventoryConfiguration: InventoryConfiguration): SetBucketInventoryConfigurationResult = ???

  override def setBucketInventoryConfiguration(setBucketInventoryConfigurationRequest: SetBucketInventoryConfigurationRequest): SetBucketInventoryConfigurationResult = ???

  override def listBucketInventoryConfigurations(listBucketInventoryConfigurationsRequest: ListBucketInventoryConfigurationsRequest): ListBucketInventoryConfigurationsResult = ???

  override def shutdown(): Unit = ???

  override def getRegion: model.Region = ???

  override def getRegionName: String = ???

  override def getUrl(s: String, s1: String): URL = ???

  override def waiters(): AmazonS3Waiters = ???

}
