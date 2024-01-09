package fi.oph.kouta.mocks

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.config.S3Configuration
import fi.oph.kouta.images.S3ImageService
import fi.oph.kouta.integration.fixture.MockS3Client

object MockS3ImageService extends S3ImageService(MockS3Client, new AuditLog(MockAuditLogger)) {
  override lazy val config =
    S3Configuration("konfo-files", "https://konfo-files.untuvaopintopolku.fi", "opintopolku-siirtotiedostot", None)
}
