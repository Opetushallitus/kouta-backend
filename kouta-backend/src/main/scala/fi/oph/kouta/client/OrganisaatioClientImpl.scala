package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory

object OrganisaatioClientImpl extends OrganisaatioClient(KoutaConfigurationFactory.configuration.urlProperties, "kouta-backend")
