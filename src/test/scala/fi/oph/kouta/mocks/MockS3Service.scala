package fi.oph.kouta.mocks

import fi.oph.kouta.config.S3Configuration
import fi.oph.kouta.indexing.S3Service
import fi.oph.kouta.integration.fixture.MockS3Client


object MockS3Service extends S3Service(MockS3Client) {
  override lazy val config = S3Configuration("konfo-files", "https://testibucket.fi", None)
}
