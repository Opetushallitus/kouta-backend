package fi.oph.kouta.service

import fi.oph.kouta.domain.oid.OrganisaatioOid

case class OrganizationAuthorizationFailedException(message:String)
  extends RuntimeException(message)

object OrganizationAuthorizationFailedException {
  def apply(allowedOrganizationOids: Iterable[OrganisaatioOid], authorizedOrganizations: Iterable[OrganisaatioOid], organizationsAuthorizationMode: OrganizationsAuthorizationMode = Any): OrganizationAuthorizationFailedException = {
    val organizationAccessDescription = if (organizationsAuthorizationMode == Any) "any of" else "all of"
    OrganizationAuthorizationFailedException(s"Authorization failed, missing organization. " +
      s"User asked right to ${organizationAccessDescription} these organizations: ${allowedOrganizationOids.map(_.s).mkString(",")}. " +
      s"Instead, authorized organizations are: ${authorizedOrganizations.map(_.s).mkString(",")}.")
  }

  def apply(missingOrganizationOid: OrganisaatioOid): OrganizationAuthorizationFailedException =
    OrganizationAuthorizationFailedException(s"Authorization failed, unknown organization oid ${missingOrganizationOid.s}")
}
